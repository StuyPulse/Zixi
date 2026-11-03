import org.wpilib.deployutils.deploy.artifact.FileTreeArtifact
import org.wpilib.gradlerio.deploy.systemcore.SystemCore
import org.wpilib.gradlerio.deploy.systemcore.WPILibJavaArtifact
import org.wpilib.toolchain.NativePlatforms

plugins {
    id("java")
    id("application")
    id("org.wpilib.GradleRIO").version("2027.0.0-alpha-8")
    id("com.peterabeles.gversion").version("1.10")
    id("com.diffplug.spotless").version("8.9.0")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

/*
 * Deploy
 */
deploy.targets.register<SystemCore>("systemcore") {
    team = wpilib.getTeamNumber()
    useDefaultSystemcoreHostName()
    debug = wpilib.getDebugOrDefault(false)

    artifacts.register<WPILibJavaArtifact>("wpilibJava") {
        // Set to true to use debug including JNI, which will drastically impact performance.
        debugJni = false
        jvmArgs.add("-XX:+UseCompactObjectHeaders")
    }

    artifacts.register<FileTreeArtifact>("wpilibStaticFileDeploy") {
        files = project.fileTree("src/main/deploy")
        directory = "/home/systemcore/deploy"
        deleteOldFiles = false
    }
}

/*
 * Sim
 */
wpi.java.runSimWithDebugJni = false // Set to true to use debug for simulation including JNI
val includeDesktopSupport = true // Set this to true to enable desktop support. Simulation configuration (e.g. environment variables).
wpi.sim.addGui().defaultEnabled = true
wpi.sim.addDriverstation()

/*
 * Dependencies
 */
val akitVersion = (groovy.json.JsonSlurper().parse(file("vendordeps/AdvantageKit.json")) as Map<*, *>)["version"]

dependencies {
    wpi.java.deps.wpilibAnnotations().forEach { annotationProcessor(it) }
    wpi.java.deps.wpilib().forEach { implementation(it) }
    wpi.java.vendor.java().forEach { implementation(it) }

    wpi.java.deps.wpilibJniDebug(NativePlatforms.systemcore).forEach { "systemcoreDebug"(it) }
    wpi.java.vendor.jniDebug(NativePlatforms.systemcore).forEach { "systemcoreDebug"(it) }

    wpi.java.deps.wpilibJniRelease(NativePlatforms.systemcore).forEach { "systemcoreRelease"(it) }
    wpi.java.vendor.jniRelease(NativePlatforms.systemcore).forEach { "systemcoreRelease"(it) }

    wpi.java.deps.wpilibJniDebug(NativePlatforms.desktop).forEach { "nativeDebug"(it) }
    wpi.java.vendor.jniDebug(NativePlatforms.desktop).forEach {"nativeDebug"(it) }
    wpi.sim.enableDebug().forEach { "simulationDebug"(it) }

    wpi.java.deps.wpilibJniRelease(NativePlatforms.desktop).forEach { "nativeRelease"(it) }
    wpi.java.vendor.jniRelease(NativePlatforms.desktop).forEach { "nativeRelease"(it) }
    wpi.sim.enableRelease().forEach {"simulationRelease"(it) }

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    annotationProcessor("org.littletonrobotics.akit:akit-autolog:$akitVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.4")
}

/*
 * Default tasks
 */
tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}

tasks.jar {
    from("src") {
        into("backup/src")
    }
    from("vendordeps") {
        into("backup/vendordeps")
    }
    from("build.gradle.kts") {
        into("backup")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-XDstringConcat=inline") // Configure string concat to always inline compile
}

fun isFullyCommented(f: File): Boolean {
    val stripped = f.readText() // for ignoring commented out files with spotless
        .replace(Regex("""(?s)/\*.*?\*/"""), "")  // block comments (including license headers)
        .replace(Regex("""(?m)//.*$"""), "")      // line comments
    return stripped.isBlank()
}

tasks.compileJava {
    dependsOn(tasks.spotlessApply)
}

spotless {
    java {
        targetExclude(
            fileTree("src")
                .matching(Action<PatternFilterable> { include("**/*.java") })
                .filter(Spec<File> { file -> isFullyCommented(file) })
        )
        
        // Update this license header to match the robot project
        licenseHeader("""/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
""").updateYearWithLatest(false)

        googleJavaFormat().aosp()
        
        trimTrailingWhitespace()
        endWithNewline()

        importOrder(
            "com.stuypulse.constants", 
            "com.stuypulse.subsystems", 
            "com.stuypulse.commands", 
            "com.stuypulse.commands.auton", 
            "com.stuypulse.util", 
            "com.stuypulse", 
            "org.wpilib", 
            "com.ctre",
            "com.revrobotics"
        )
        removeUnusedImports()
    }
}

/*
 * Application Setup
 */
val ROBOT_MAIN_CLASS = "com.stuypulse.robot.Main"
application.mainClass.set(ROBOT_MAIN_CLASS)
wpi.java.configureApplication(application)

val deployArtifact = deploy.targets["systemcore"].artifacts["wpilibJava"] as WPILibJavaArtifact
deployArtifact.configureApplication(application)
tasks.test {
    wpi.java.configureTestTasks(this)
}

/*
 * Custom tasks
 */
sourceSets { // isolated sourceset to avoid circular dep
    create("tools") // defaulted to src/tools/java
}

dependencies {
    "toolsImplementation"("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")
}

gversion {
    srcDir       = "src/main/java/"
    classPackage = "com.stuypulse.robot.generated"
    className    = "BuildConstants"
    dateFormat   = "yyyy-MM-dd HH:mm:ss z"
    timeZone     = "America/New_York"
    indent       = "  "
}

val patchBuildConstants = tasks.register<JavaExec>("patchBuildConstants") {
    dependsOn(tasks.named("createVersionFile"))

    classpath = sourceSets["tools"].runtimeClasspath
    mainClass = "deployment.PatchBuildConstants" // appended to the classpath first tho so it's src/tools/java/deployment/PatchBuildConstants

    doFirst {
        args(file(gversion.srcDir).absolutePath, gversion.classPackage, gversion.className, gversion.indent, file(".git").absolutePath)
    }
}

tasks.compileJava {
    dependsOn(patchBuildConstants)
}

tasks.javadoc {
    val opt = options as StandardJavadocDocletOptions
    opt.addBooleanOption("linksource", true)
    opt.addBooleanOption("private", true)
    opt.addBooleanOption("-allow-script-in-comments", true)

    opt.addStringOption("doctitle", "Zixi")

    exclude("${gversion.classPackage.replace(".", "/")}/${gversion.className}.java")
}
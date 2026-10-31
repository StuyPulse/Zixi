import org.wpilib.gradlerio.deploy.systemcore.SystemCore;
import org.wpilib.gradlerio.deploy.systemcore.WPILibJavaArtifact;
import org.wpilib.deployutils.deploy.artifact.FileTreeArtifact;
import org.wpilib.toolchain.NativePlatforms;
import java.io.File;
import org.gradle.api.tasks.compile.JavaCompile;

plugins {
    id("java")
    id("application")
    id("org.wpilib.GradleRIO").version("2027.0.0-alpha-8")
    id("com.diffplug.spotless").version("8.9.0")
}


java {
    sourceCompatibility = JavaVersion.VERSION_25;
    targetCompatibility = JavaVersion.VERSION_25;
}

val ROBOT_MAIN_CLASS = "com.stuypulse.robot.Main";

// Define my targets (SystemCore) and artifacts (deployable files)
// This is added by GradleRIO's backing project DeployUtils.
// deploy {
//     targets {
//         register<SystemCore>(deploy.getTargetTypeClass("SystemCore")) {
//             // Team number is loaded either from the .wpilib/wpilib_preferences.json
//             // or from command line. If not found an exception will be thrown.
//             // You can use getTeamOrDefault(team) instead of getTeamNumber if you
//             // want to store a team number in this file.
//             setProperty("team", project.wpilib.getTeamNumber());
//             // Use the default systemcore host name. This must be called after setting team
//             // as happens on the line above
//             "useDefaultSystemcoreHostName"();
//             setProperty("debug", project.wpilib.getDebugOrDefault(false));

//             "artifacts" {
//                 // First part is artifact name, 2nd is artifact type
//                 // getTargetTypeClass is a shortcut to get the class type using a string

//                 "wpilibJava"(getArtifactTypeClass("WPILibJavaArtifact")) {
//                     // Set to true to use debug including JNI, which will drastically impact performance.
//                     setProperty("debugJni", false);
//                     jvmArgs.add("-XX:+UseCompactObjectHeaders");
//                 }

//                 // Static files artifact
//                 "wpilibStaticFileDeploy"(getArtifactTypeClass("FileTreeArtifact")) {
//                     setProperty("files", project.fileTree("src/main/deploy"));
//                     setProperty("directory", "/home/systemcore/deploy");
//                     setProperty("deleteOldFiles", false); // Change to true to delete files on systemcore that no
//                                            // longer exist in deploy directory of this project
//                 }
//             }
//         }
//     }
// }
deploy.targets.register<SystemCore>("systemcore") {
    team = wpilib.getTeamNumber();
    useDefaultSystemcoreHostName();
    debug = wpilib.getDebugOrDefault(false);

    artifacts.register<WPILibJavaArtifact>("wpilibJava") {
        // Set to true to use debug including JNI, which will drastically impact performance.
        debugJni = false;
        jvmArgs.add("-XX:+UseCompactObjectHeaders");
    }

    artifacts.register<FileTreeArtifact>("wpilibStaticFileDeploy") {
        files = project.fileTree("src/main/deploy")
        directory = "/home/systemcore/deploy"
        deleteOldFiles = false
    }
}

val deployArtifact = deploy.targets["systemcore"].artifacts["wpilibJava"] as WPILibJavaArtifact;

// Set to true to use debug for simulation including JNI
wpi.java.runSimWithDebugJni = false;

// Set this to true to enable desktop support.
val includeDesktopSupport = true;

// Defining my dependencies. In this case, WPILib (+ friends), and vendor libraries.
// Also defines JUnit 5.
dependencies {
    wpi.java.deps.wpilibAnnotations().forEach { annotationProcessor(it) };
    wpi.java.deps.wpilib().forEach { implementation(it) };
    wpi.java.vendor.java().forEach { implementation(it) };

    wpi.java.deps.wpilibJniDebug(NativePlatforms.systemcore).forEach { "systemcoreDebug"(it) };
    wpi.java.vendor.jniDebug(NativePlatforms.systemcore).forEach { "systemcoreDebug"(it) };

    wpi.java.deps.wpilibJniRelease(NativePlatforms.systemcore).forEach { "systemcoreRelease"(it) };
    wpi.java.vendor.jniRelease(NativePlatforms.systemcore).forEach { "systemcoreRelease"(it) };

    wpi.java.deps.wpilibJniDebug(NativePlatforms.desktop).forEach { "nativeDebug"(it) };
    wpi.java.vendor.jniDebug(NativePlatforms.desktop).forEach {"nativeDebug"(it) };
    wpi.sim.enableDebug().forEach { "simulationDebug"(it) };

    wpi.java.deps.wpilibJniRelease(NativePlatforms.desktop).forEach { "nativeRelease"(it) };
    wpi.java.vendor.jniRelease(NativePlatforms.desktop).forEach { "nativeRelease"(it) };
    wpi.sim.enableRelease().forEach {"simulationRelease"(it) };

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1");
    testRuntimeOnly("org.junit.platform:junit-platform-launcher");

    val akitJson = groovy.json.JsonSlurper().parse(File(projectDir.getAbsolutePath() + "/vendordeps/AdvantageKit.json")) as Map<*, *>;
    "annotationProcessor"("org.littletonrobotics.akit:akit-autolog:${akitJson["version"]}");

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2");
}

tasks.test {
    useJUnitPlatform();
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true");
    // jvmArgs "--add-opens=java.base/jdk.internal.vm=ALL-UNNAMED" // allow tests to access robot code classes
}

// Simulation configuration (e.g. environment variables).
wpi.sim.addGui().defaultEnabled = true;
wpi.sim.addDriverstation();

application.mainClass.set(ROBOT_MAIN_CLASS);

deployArtifact.configureApplication(application);
wpi.java.configureApplication(application);
wpi.java.configureTestTasks(tasks.test.get());

tasks.jar {
    from("src") {
        into("backup/src");
    }
    from("vendordeps") {
        into("backup/vendordeps");
    }
    from("build.gradle") {
        into("backup");
    }
}

// Configure string concat to always inline compile
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-XDstringConcat=inline");
}

// True if the file has nothing left after stripping comments
// For ignoring commented out files with spotless
fun isFullyCommented(f: File): Boolean {
    val stripped = f.readText()
        .replace(Regex("""(?s)/\*.*?\*/"""), "")  // block comments (including license headers)
        .replace(Regex("""(?m)//.*$"""), "")      // line comments
    return stripped.isBlank()
}

tasks.compileJava {
    dependsOn(tasks.spotlessApply);
}
spotless {
    java {
        targetExclude(
            fileTree("src")
                .matching(Action<PatternFilterable> { include("**/*.java") })
                .filter(Spec<File> { file -> isFullyCommented(file) })
        );
        
        // Update this license header to match the robot project
        licenseHeader("""/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
""").updateYearWithLatest(false);

        googleJavaFormat().aosp();
        
        trimTrailingWhitespace();
        endWithNewline();

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
        );
        removeUnusedImports();
    }
}

tasks.register<JavaExec>("recordLastCommit") {
    group = "deployment";

    classpath = sourceSets["main"].runtimeClasspath;
    mainClass = "tools.deployment.RecordLastCommitTask";

    workingDir = project.rootDir;
}

tasks.named("deploy") {
    dependsOn(tasks.named("recordLastCommit"));
}

tasks.named("simulateExternalJava") {
    dependsOn(tasks.named("recordLastCommit"));
}
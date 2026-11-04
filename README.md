<div align="center">
<a href="https://git.io/typing-svg"><img src="https://readme-typing-svg.herokuapp.com?font=Tiny5+Duo&size=48&duration=2500&pause=1000&color=F7F7F7&background=FF0000&center=true&vCenter=true&width=870&height=150&lines=Zixi;A+StuyPulse+Template;Dedicated+to+Zixi+Qiao" alt="Typing SVG" /></a></a>
</div>
<div align="center">
<a href="https://github.com/StuyPulse/Zixi/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-750014?style=for-the-badge&logo=markdown"></a>
<a><img src="https://img.shields.io/github/actions/workflow/status/StuyPulse/StuyPlus-2026/gradle.yml?style=for-the-badge&label=Build"></a>
<!-- <br> -->
<a href="https://github.com/StuyPulse/Zixi/tree/main/src/main/java/com/stuypulse/robot"><img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk"></a>
<a href="https://github.com/wpilibsuite/allwpilib"><img src="https://img.shields.io/badge/WPILib-Alpha_8-aa182c?style=for-the-badge&logo=first&labelColor=4d4e4e"></a>
</div>

## About Zixi

This is a robot template designed with the IO architecture and use by other teams in mind. It was created out of a desire to overhaul the old template, [Phil](https://github.com/StuyPulse/Phil/), which was oriented towards StuyLib and Commands v2, while Zixi is intended to be used with AdvantageKit and Commands v3.

## To Use

- Click `Use this template` in GitHub and create your new repository using the `Zixi` template. 
- Ensure that the `vendordeps` & `build.gradle`'s plugin versions are up-to-date in your new repository.
- Change the project name in `build.gradle`'s file header to match your project.
- Happy coding! 😀

## Features

Zixi utilizes a [Kotlin gradle script](https://github.com/StuyPulse/Zixi/blob/main/build.gradle.kts), chosen for its modernity and relation to Java. We configured Gradle such that Google Java format using AOSP runs every build, keeping our code cleanly formatted. It also utilizes gversion to insert commit metadata into robot logs, helping link match logs to the code version running on the robot. Finally, we use JUnit testing to prevent most crashes before they reach the robot.

Due to the vast amount of custom utilities provided in this template, Javadoc is available [here](https://stuypulse.github.io/Zixi/index.html). These utilities can be found in the `com.stuypulse.robot.util` package, primarily including TalonFX extensions for easier logging through AdvantageKit and logically coupled simulations.

## To Maintain
Do *not* use the automatic import project tool. Instead,
- Change the 'frcYear' to the current year in `settings.gradle`.
- Check that the `GradleRIO` version is up to date in `./build.gradle`. 
- Update all dependencies using WPILib VSCode's action bar `Vendor Dependencies` menu.

## Contact & Questions

If you have any ideas for improvement, questions, or problems experienced, [create an issue](https://github.com/StuyPulse/Zixi/issues/new)! Contributions via fork & pull request are also welcome.

/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/8.0/userguide/multi_project_builds.html
 */

rootProject.name = "bytecoder-gradle"
include("plugin")
pluginManagement {
    repositories {
        mavenCentral()
    }
    buildscript{
        dependencies{
            classpath("de.mirkosertic.bytecoder:bytecoder-cli:2023-05-19")
        }
    }
}

buildscript{
    repositories {
        mavenCentral()
    }
    dependencies{
        classpath("de.mirkosertic.bytecoder:bytecoder-cli:2023-05-19")
    }
}
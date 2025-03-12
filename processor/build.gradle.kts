plugins {
    java
}

group = "eu.nitonfx.signaling"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    implementation(project(":lib"))

    implementation("com.niton.compile:proto")
    implementation("com.squareup:javapoet:1.13.0")
}

repositories {
    mavenCentral()
}

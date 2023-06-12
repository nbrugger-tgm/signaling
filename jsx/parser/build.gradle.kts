plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    api("com.niton.jainparse:core")
}
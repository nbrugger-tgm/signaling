plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":lib"))
}
plugins {
    id("java")
}

group = "com.niton"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":lib"))
}
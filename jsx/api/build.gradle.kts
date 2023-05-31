plugins {
    id("java-library")
}

group = "com.niton"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":lib"))
}
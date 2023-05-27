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
    implementation("org.teavm:teavm-jso-apis:0.8.1")
}
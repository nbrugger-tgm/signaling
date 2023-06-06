plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":lib"))
    implementation("org.teavm:teavm-jso-apis:0.8.1")
}
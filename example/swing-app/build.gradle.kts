plugins {
    id("java")
    id("application")
}

group = "eu.nitonfx.signaling"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(project(":processor"))
    implementation(project(":processor"))

    implementation(project(":lib"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("eu.niton.signaling.App")
}

tasks.test {
    useJUnitPlatform()
}
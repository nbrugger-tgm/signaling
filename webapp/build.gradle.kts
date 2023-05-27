import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("java")
    id("org.teavm").version("0.8.1")
}

group = "com.niton"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://niton.jfrog.io/artifactory/java-libs/")
}

dependencies {
    teavm(project(":jsx:processor"))//temporary
    implementation("com.niton.jainparse:core:3.0.0b1")//temp

    annotationProcessor(project(":jsx:processor"))
    compileOnly(project(":jsx:api"))
    teavm(project(":jsx:runtime"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
teavm.js {
    addedToWebApp.set(true)
    mainClass.set("example.MainClass")
    debugInformation.set(true)
    obfuscated.set(false)
    sourceMap.set(true)
    optimization.set(OptimizationLevel.NONE)
    // this is also optional, default value is <project name>.js
    targetFileName.set("example.js")
}
tasks.register("package"){
    dependsOn("generateJavaScript");
    dependsOn("processResources");
    doLast {
        copy {
            from("$buildDir/generated/teavm")
            into("$buildDir/app")
        }
        copy {
            from("$buildDir/resources/main")
            into("$buildDir/app")
        }.getDidWork()
    }
    outputs.dir("$buildDir/app")
}
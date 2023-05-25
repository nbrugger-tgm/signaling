plugins {
    id("java")
    id("org.teavm").version("0.8.1")
}

group = "com.niton"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    teavm(project(":lib"))
    teavm(teavm.libs.jsoApis)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
teavm.js {
    addedToWebApp.set(true)
    mainClass.set("example.MainClass")

    // this is also optional, default value is <project name>.js
    targetFileName.set("example.js")
}
tasks.register("package"){
    dependsOn("generateJavaScript");
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
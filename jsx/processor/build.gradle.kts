plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven("https://niton.jfrog.io/artifactory/java-libs/")
}
tasks.withType(JavaCompile::class.java) {
    options.compilerArgs.add("--enable-preview")
}
dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.0")
    compileOnly("com.google.auto.service:auto-service:1.1.0")

    implementation(project(":jsx:api"))
    api(project(":jsx:parser"))

    //just for type references -> type save generation
    implementation(project(":lib"))
    implementation(project(":jsx:runtime"))
    implementation("org.teavm:teavm-jso-apis:0.8.1")

    compileOnly("org.jetbrains:annotations:24.0.1")

    implementation("com.niton.compile:proto:1.0-a0")
    implementation("com.squareup:javapoet:1.+")
}

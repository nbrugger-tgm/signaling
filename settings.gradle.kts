rootProject.name = "signaling"
include("lib")
include("example:swing-app")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.9.0")
}
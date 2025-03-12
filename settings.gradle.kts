rootProject.name = "signaling"
include("lib")
include("processor")
include("example:swing-app")

includeBuild("dependencies/proto")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.9.0")
}
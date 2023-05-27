rootProject.name = "signaling"
includeBuild("jsx/parser") {
    dependencySubstitution {
        substitute(module("com.niton.jainparse:core")).using(project(":core"))
    }
}
include("lib", "webapp")
include("jsx:processor","jsx:runtime","jsx:api")


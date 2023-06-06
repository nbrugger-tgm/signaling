rootProject.name = "signaling"
includeBuild("jainparse"){
    dependencySubstitution {
        substitute(module("com.niton.jainparse:core")).using(project(":core"))
    }
}
include("lib", "webapp")
include("jsx:processor","jsx:runtime","jsx:api")
include("jsx:parser")
findProject(":jsx:parser")?.name = "parser"

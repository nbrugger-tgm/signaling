rootProject.name = "signaling"
includeBuild("bytecoder-gradle")
includeBuild("bytecoder")
includeBuild("jsx/parser")
include("lib", "webapp")
include("jsx:processor","jsx:runtime","jsx:api")

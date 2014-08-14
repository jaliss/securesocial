name := "SecureSocial-parent"

version := Common.version

lazy val core =  project.in( file("module-code") )

lazy val testKit= project.in( file("test-kit") ).dependsOn(core)

lazy val scalaDemo = project.in( file("samples/scala/demo") ).dependsOn(core,testKit)

lazy val javaDemo = project.in( file("samples/java/demo") ).dependsOn(core)


lazy val root = project.in( file(".") ).aggregate(core, scalaDemo, javaDemo, testKit).settings(
     aggregate in update := false
)

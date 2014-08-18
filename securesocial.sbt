name := "SecureSocial-parent"

version := Common.version

scalaVersion := Common.scalaVersion

lazy val core =  project.in( file("module-code") ).enablePlugins(PlayScala)

lazy val testKit= project.in( file("test-kit") ).dependsOn(core)

lazy val scalaDemo = project.in( file("samples/scala/demo") ).enablePlugins(PlayScala).dependsOn(core,testKit)

lazy val javaDemo = project.in( file("samples/java/demo") ).enablePlugins(PlayJava).dependsOn(core)


lazy val root = project.in( file(".") ).aggregate(core, scalaDemo, javaDemo, testKit).settings(
     aggregate in update := false
)

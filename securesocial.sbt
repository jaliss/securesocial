name := "SecureSocial-parent"

version := Common.version

scalaVersion := Common.scalaVersion

lazy val core =  project.in( file("module-code") ).enablePlugins(PlayScala)

lazy val scalaDemo = project.in( file("samples/scala/demo") ).enablePlugins(PlayScala).dependsOn(core)

lazy val javaDemo = project.in( file("samples/java/demo") ).enablePlugins(PlayJava).dependsOn(core)

lazy val root = project.in( file(".") ).aggregate(core, scalaDemo, javaDemo) .settings(
     aggregate in update := false
   )

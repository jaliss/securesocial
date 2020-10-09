name := "SecureSocial-parent"

version := Common.version

scalaVersion := Common.scalaVersion
crossScalaVersions := Common.crossScalaVersions

lazy val core =  project.in( file("module-code") ).enablePlugins(PlayScala)

lazy val scalaDemo = project.in( file("samples/scala/demo") ).enablePlugins(PlayScala).dependsOn(core)

lazy val javaDemo = project.in( file("samples/java/demo") ).enablePlugins(PlayJava).dependsOn(core)

lazy val root = project.in( file(".") ).aggregate(core, scalaDemo, javaDemo) .settings(
     aggregate in update := false
   )

// skip javadoc 
// https://www.scala-sbt.org/sbt-native-packager/formats/universal.html
mappings in (Compile, packageDoc) := Seq()
sources in (Compile, doc) := Seq()
publishArtifact in (Compile, packageDoc) := false

import PlayKeys._

name    := "java-demo"

version := Common.version

scalaVersion := Common.scalaVersion

scalariformSettings

libraryDependencies ++= Seq("ws.securesocial" %% "securesocial" % version.value, javaCore)

resolvers += Resolver.sonatypeRepo("snapshots")

javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF-8", "-Xlint:-options")

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

routesImport ++= Seq("scala.language.reflectiveCalls")

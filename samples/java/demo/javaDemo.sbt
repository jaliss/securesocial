name    := "java-demo"

version := Common.version

scalaVersion := Common.scalaVersion

scalariformSettings

libraryDependencies ++= Seq("ws.securesocial" %% "securesocial" % version.value, javaCore)

resolvers += Resolver.sonatypeRepo("snapshots")

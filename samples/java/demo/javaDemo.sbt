import play.Project._

name    := "java-demo"

version := Common.version

libraryDependencies ++= Seq("ws.securesocial" %% "securesocial" % "master-SNAPSHOT", javaCore)

resolvers += Resolver.sonatypeRepo("snapshots")

playJavaSettings

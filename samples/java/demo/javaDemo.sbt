import play.Project._

name    := "java-demo"

version := Common.version

libraryDependencies ++= Seq("ws.securesocial" %% "securesocial" % version.value, javaCore)

resolvers += Resolver.sonatypeRepo("snapshots")

playJavaSettings

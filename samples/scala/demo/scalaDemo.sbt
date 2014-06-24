name    := "scala-demo"

version := Common.version

scalaVersion := Common.scalaVersion

libraryDependencies += "ws.securesocial" %% "securesocial" % version.value

resolvers += Resolver.sonatypeRepo("snapshots")


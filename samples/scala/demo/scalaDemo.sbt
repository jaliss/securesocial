name    := "scala-demo"

version := Common.version

libraryDependencies += "ws.securesocial" %% "securesocial" % "master-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

playScalaSettings

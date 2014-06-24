name    := "scala-demo"

version := Common.version

libraryDependencies += "ws.securesocial" %% "securesocial" % version.value

resolvers += Resolver.sonatypeRepo("snapshots")

playScalaSettings

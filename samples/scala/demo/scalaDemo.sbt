name    := "ssdemo-scala"

version := Common.version

libraryDependencies += "ws.securesocial" %% "securesocial" % "master-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

playScalaSettings

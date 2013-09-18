import play.Project._

name := "ssdemo-scala"

version := "1.0"

libraryDependencies ++= Seq(
  "securesocial" %% "securesocial" % "master-SNAPSHOT"
)

resolvers ++= Seq(
  Resolver.sbtPluginRepo("snapshots")
)

playScalaSettings

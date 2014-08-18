name    := "scala-demo"

version := Common.version

scalaVersion := Common.scalaVersion

libraryDependencies ++= Seq(
  filters,
  "ws.securesocial" %% "securesocial" % version.value
)

resolvers += Resolver.sonatypeRepo("snapshots")


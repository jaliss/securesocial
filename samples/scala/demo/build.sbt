import PlayKeys._

name    := "scala-demo"

version := Common.version

scalaVersion := Common.scalaVersion

crossScalaVersions := Common.crossScalaVersions

libraryDependencies ++= Seq(
  specs2 % "test",
  "ws.securesocial" %% "securesocial" % version.value,
  guice,
  ehcache
)

resolvers += Resolver.sonatypeRepo("snapshots")

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

routesImport ++= Seq("scala.language.reflectiveCalls")

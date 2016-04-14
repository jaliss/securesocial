name    := "scala-demo"

version := Common.version

scalaVersion := Common.scalaVersion

scalariformSettings

libraryDependencies ++= Seq(
  specs2 % "test",
  "ws.securesocial" %% "securesocial" % version.value,
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "com.typesafe.play" %% "play-mailer" % "3.0.1"
)

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"


scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

routesImport ++= Seq("scala.language.reflectiveCalls")

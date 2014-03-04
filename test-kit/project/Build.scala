import sbt._
import Keys._

object ApplicationBuild extends Build {

    val appName         = "securesocial-testkit"
    val appVersion      = "master-SNAPSHOT"
    val appOrganization    = "ws.securesocial"
    val dependencies = Seq(
	    "ws.securesocial" %% "securesocial" % "master-SNAPSHOT",
      "org.scalacheck" %% "scalacheck" % "1.11.1",
      "com.typesafe.play" %% "play-test" % "2.2.0",
      "org.mockito" % "mockito-all" % "1.9.5"
  )
    val main = sbt.Project(appName, file(".") ).settings(
      resolvers += Resolver.sonatypeRepo("snapshots")
    , libraryDependencies ++= dependencies
    , version := appVersion
    , organization := appOrganization
    )
}

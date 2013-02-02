import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  val appName         = "securesocial"
  val appVersion      = "2.1-RC4"

  organization  := "com.micronautics" // Don't want this fork to step on securesocial

  crossPaths    := false
  publishMavenStyle := false

  val appDependencies = Seq(
    "com.typesafe" %% "play-plugins-util" % "2.1-RC2",   // not released for Play 2.1-RC4 yet
    "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2", // not released for Play 2.1-RC4 yet
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers ++= Seq(
      "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    ),

    publishTo := Some(Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns))
  )
}

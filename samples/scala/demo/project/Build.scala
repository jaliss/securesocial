import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "ssdemo-scala"
  val appVersion      = "1.0-SNAPSHOT"

  lazy val secureSocialDeps = Seq(
    // "com.typesafe" %% "play-plugins-util" % "2.0.1", // notTransitive(), — not available yet for Scala 2.10? (as of December 19, 2012 - and certainly not in October 2012.)
    // "com.typesafe" %% "play-plugins-mailer" % "2.1-RC1", is this available anywhere?
    "org.mindrot" % "jbcrypt" % "0.3m")

  lazy val secureSocial =
    play.Project("securesocial", appVersion, secureSocialDeps,
      path = file("modules/securesocial")
    ).settings(
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")
    )

  val main = play.Project(appName, appVersion).settings(
      resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/snapshots/"))(Resolver.ivyStylePatterns)
    ) dependsOn (
      secureSocial
    )

}

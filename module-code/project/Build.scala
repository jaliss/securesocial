import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "securesocial"
    val appVersion      = "0.9.29-SNAPSHOT"

    val appDependencies = Seq(
      "com.typesafe" %% "play-plugins-util" % "2.1.0",
      "com.typesafe" %% "play-plugins-mailer" % "2.1.0",
      "org.mindrot" % "jbcrypt" % "0.3m"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
    ).settings(
      publishMavenStyle := false,
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo <<= version { (version: String) =>
      val localPublishRepo = "/Users/dorel/Work/maven-repos"
      if (version.trim.endsWith("SNAPSHOT"))
        Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
      else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
      },
      publishMavenStyle := true,
      crossVersion := CrossVersion.full
    )

}

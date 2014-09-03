import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._

object ApplicationBuild extends Build {

    val appName         = "securesocial"
    val appVersion      = "master-20140905"

    val appDependencies = Seq(
      cache, ws, javaWs, json,
      "com.typesafe.play.plugins" %% "play-plugins-util" % "2.3.0",
      "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
      "org.mindrot" % "jbcrypt" % "0.3m"
    )

    val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
      version := appVersion,
      libraryDependencies ++= appDependencies
    ).settings(
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo := Some(Resolver.file("file", new File("../../../sonatype-work/nexus/storage/towel")))
    )

}

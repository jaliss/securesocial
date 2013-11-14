import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "securesocial"
    val appVersion      = "2.2.0.1-SCM"

    val appDependencies = Seq(
      "com.typesafe" %% "play-plugins-util" % "2.2.0",
      "com.typesafe" %% "play-plugins-mailer" % "2.2.0",
      "org.mindrot" % "jbcrypt" % "0.3m",
      cache
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
    ).settings(
      publishMavenStyle := false,
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo <<= (version) { version: String =>

        val rootDir = "/srv/maven/"
        val path =
          if (version.trim.endsWith("SNAPSHOT"))
            rootDir + "snapshots/"
          else
            rootDir + "releases/"

        Some(Resolver.sftp("scm.io intern repo", "scm.io", 44144, path))
      }
    )

}

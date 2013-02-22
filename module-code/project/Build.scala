import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "securesocial"
    val appVersion      = "2.0.11"

    val appDependencies = Seq(
      "com.typesafe" %% "play-plugins-util" % "2.0.3",
      "com.typesafe" %% "play-plugins-mailer" % "2.0.4",
      "org.mindrot" % "jbcrypt" % "0.3m"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      publishMavenStyle := false,
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      publishTo <<= (version) { version: String =>
        val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
        val (name, url) = if (version.contains("-SNAPSHOT"))
          ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
        else
          ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
         Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
      }
    )
}

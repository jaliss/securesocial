import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  val appName         = "securesocial"
  val appVersion      = "2.1-RC4"

  val appDependencies = Seq(
    "com.typesafe" %% "play-plugins-util" % "2.1-RC2",   // not released for Play 2.1-RC4 yet
    "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2", // not released for Play 2.1-RC4 yet
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
  ).settings(
    resolvers ++= Seq(
      "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    )
  )

  publishTo <<= (version) { version: String =>
     val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
     val (name, url) = if (version.contains("-SNAPSHOT"))
                         ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                       else
                         ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
     Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
  }
}

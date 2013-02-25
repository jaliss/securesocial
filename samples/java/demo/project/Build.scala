import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-java"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "securesocial" %% "securesocial" % "2.0.12"
    )
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
	resolvers += Resolver.url("sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    )

}

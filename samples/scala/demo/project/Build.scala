import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-scala"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
	"securesocial" %% "securesocial" % "2.0.11"
    )
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
	resolvers += Resolver.url("sbt-plugin-releases", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    )

}

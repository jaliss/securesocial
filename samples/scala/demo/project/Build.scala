import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-scala"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
	"securesocial" %% "securesocial" % "2.0-SNAPSHOT"
    )
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
	resolvers += Resolver.url("sbt-plugin-snapshots", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    )

}

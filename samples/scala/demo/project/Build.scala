import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-scala"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
	"securesocial" % "securesocial_2.9.1" % "master"
    )
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/snapshots/"))(Resolver.ivyStylePatterns)
    )

}

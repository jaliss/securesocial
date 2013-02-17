import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-java"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "securesocial" % "securesocial_2.10" % "master"
    )
    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/snapshots/"))(Resolver.ivyStylePatterns)
    )

}

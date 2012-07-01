import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-scala"
    val appVersion      = "1.0-SNAPSHOT"

    val ssDependencies = Seq(
    )
 
    val appDependencies = Seq(
            "securesocial" %% "securesocial" % "1.0-SNAPSHOT"
    )
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      )
}

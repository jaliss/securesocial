import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-java"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val secureSocial = PlayProject(
    	"securesocial2", appVersion, mainLang = SCALA, path = file("modules/securesocial")
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    ).dependsOn(secureSocial).aggregate(secureSocial)

}

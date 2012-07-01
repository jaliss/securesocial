import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "securesocial"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
        "com.typesafe" %% "play-plugins-util" % "2.0.1",
        "org.mindrot" % "jbcrypt" % "0.3m"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here 
        resolvers ++= Seq(
            "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
            "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
        )
    )

}

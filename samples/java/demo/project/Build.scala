import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-java"
    val appVersion      = "1.0-SNAPSHOT"

    val ssDependencies = Seq(
      // Add your project dependencies here,
      "com.typesafe" %% "play-plugins-util" % "2.0.1",
       "com.typesafe" %% "play-plugins-mailer" % "2.0.4",
      "org.mindrot" % "jbcrypt" % "0.3m"
    )
 
    val secureSocial = PlayProject(
    	"securesocial", appVersion, ssDependencies, mainLang = SCALA, path = file("modules/securesocial")
    ).settings(
      resolvers ++= Seq(
        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      )
    )

    val appDependencies = Seq()
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    ).dependsOn(secureSocial).aggregate(secureSocial)

}

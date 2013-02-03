import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
    val appName         = "ssdemo-scala"
    val appVersion      = "1.0-SNAPSHOT"

    scalaVersion := "2.10.0"

    val appDependencies = Seq(
    	"com.micronautics" % "securesocial" % "2.1-RC4a" withSources()
    )
    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("play-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("play-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    )

}

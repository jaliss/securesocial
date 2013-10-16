import play.Project._

name := "securesocial"

version := "master-SNAPSHOT"

libraryDependencies ++= Seq(
  cache,
  "com.typesafe" %% "play-plugins-util" % "2.2.0",
  "com.typesafe" %% "play-plugins-mailer" % "2.2.0",
  "org.mindrot" % "jbcrypt" % "0.3m"
)

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)

publishMavenStyle := false

publishTo <<= (version) { v: String =>
  val status = if(v.trim.endsWith("-SNAPSHOT")) "snapshots" else "releases"
  Some(Resolver.sbtPluginRepo(status))
}

playScalaSettings


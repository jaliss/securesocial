import play.Project._

name := "securesocial"

version := "master-SNAPSHOT"

libraryDependencies ++= Seq(
  cache,
  "com.typesafe" %% "play-plugins-util" % "2.1.0",
  "com.typesafe" %% "play-plugins-mailer" % "2.1.0",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.fasterxml.jackson" % "jackson-mapper-asl" % "1.9.13"
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

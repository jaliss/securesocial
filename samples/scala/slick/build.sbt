name := "slick-demo"

version := "1.0-SNAPSHOT"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:reflectiveCalls" // Using -> in routes causes scala compiler to complain, this fixes it
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.url("sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
)

libraryDependencies ++= Seq(
  jdbc,
  "com.typesafe.play" %% "play-slick" % "0.6.0-SNAPSHOT",
  "mysql" % "mysql-connector-java" % "5.1.28",
  "ws.securesocial" %% "securesocial" % "master-SNAPSHOT"
)     

play.Project.playScalaSettings

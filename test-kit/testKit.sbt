name    := "test-kit"

version := Common.version

scalaVersion := Common.scalaVersion

libraryDependencies ++= Seq(
  "ws.securesocial" %% "securesocial" % version.value,
  "org.scalacheck" %% "scalacheck" % "1.11.1",
  "com.typesafe.play" %% "play-test" % Common.playVersion, 
  "org.mockito" % "mockito-all" % "1.9.5"
)

resolvers += Resolver.sonatypeRepo("snapshots")

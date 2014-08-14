name    := "test-kit"

version := Common.version

libraryDependencies ++= Seq(
  "ws.securesocial" %% "securesocial" % version.value,
  "org.scalacheck" %% "scalacheck" % "1.11.1",
  "com.typesafe.play" %% "play-test" % "2.2.0",
  "org.mockito" % "mockito-all" % "1.9.5"
)

resolvers += Resolver.sonatypeRepo("snapshots")

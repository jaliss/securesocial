organization := "io.github.secsoc"

name := "SecSoc"

version := Common.version

scalaVersion := Common.scalaVersion

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.typesafe.play.plugins" %% "play-plugins-util" % "2.3.0",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.specs2" %% "specs2" % "2.3.12" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

//scalariformSettings
seq(bintrayPublishSettings:_*)

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)


organizationName := "SecSoc"


publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }



startYear := Some(2015)

description := """An authentication module for Play Framework applications supporting OAuth, OAuth2, OpenID, Username/Password and custom authentication schemes.
                  It's lite fork of SecureSocial project, that exclude any default UI and Java support. It's allow only core functions of authentications.
               """

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://leonidv.github.io/secsoc"))

pomExtra := (
  <scm>
    <url>https://github.com/leonidv/secsoc</url>
    <connection>scm:git:git@github.com:leonidv/secsoc.git</connection>
    <developerConnection>scm:git:https://github.com/leonidv/secsoc.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>leonidv</id>
      <name>Leonid Vygovskiy</name>
      <email>Leonid.Vygovskiy [at] gmail.com</email>
      <url>http://vygovskiy.com</url>
    </developer>
    <developer>
      <id>jaliss</id>
      <name>Jorge Aliss</name>
      <email>jaliss [at] gmail.com</email>
      <url>https://twitter.com/jaliss</url>
      <roles>
        <role>Author of original SecureSocial project</role>
      </roles>
    </developer>
  </developers>
)

scalacOptions := Seq("-feature", "-deprecation")


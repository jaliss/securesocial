import play.sbt.PlayImport.PlayKeys._

name := "SecureSocial"

version := Common.version

scalaVersion := Common.scalaVersion

crossScalaVersions := Seq("2.11.7", "2.10.5")

//PlayKeys.generateRefReverseRouter := false

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  specs2 % "test",
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "org.mindrot" % "jbcrypt" % "0.3m"
)

scalariformSettings

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

organization := "ws.securesocial"

organizationName := "SecureSocial"

organizationHomepage := Some(new URL("http://www.securesocial.ws"))

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

startYear := Some(2012)

description := "An authentication module for Play Framework applications supporting OAuth, OAuth2, OpenID, Username/Password and custom authentication schemes."

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://www.securesocial.ws"))

pomExtra := (
  <scm>
    <url>https://github.com/jaliss/securesocial</url>
    <connection>scm:git:git@github.com:jaliss/securesocial.git</connection>
    <developerConnection>scm:git:https://github.com/jaliss/securesocial.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>jaliss</id>
      <name>Jorge Aliss</name>
      <email>jaliss [at] gmail.com</email>
      <url>https://twitter.com/jaliss</url>
    </developer>
  </developers>
)

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

// not adding -Xlint:unchecked for now, will do it once I improve the Java API
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8",  "-Xlint:-options")

// packagedArtifacts += ((artifact in playPackageAssets).value -> playPackageAssets.value)


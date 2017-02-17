import play.sbt.PlayImport.PlayKeys._

name := "SecureSocial"

version := Common.version

scalaVersion := Common.scalaVersion

//PlayKeys.generateRefReverseRouter := false

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  specs2 % "test",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
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

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature", "-Xmax-classfile-name","78")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8",  "-Xlint:-options", "-Xlint:unchecked", "-Xlint:deprecation" )

// packagedArtifacts += ((artifact in playPackageAssets).value -> playPackageAssets.value)


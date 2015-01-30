organization := "io.github.secsoc"

name    := "scala-demo"

version := Common.version

scalaVersion := Common.scalaVersion

//scalariformSettings

libraryDependencies += "io.github.secsoc" %% "secsoc" % version.value

resolvers += Resolver.sonatypeRepo("snapshots")


// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += Resolver.typesafeRepo("releases")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0-RC2")

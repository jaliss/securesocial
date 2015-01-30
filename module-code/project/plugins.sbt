// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("https://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % System.getProperty("play.version"))

// PGP signing
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")



addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.2")

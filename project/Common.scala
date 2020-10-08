

object Common {
  def version = "master-SNAPSHOT"  
  def playVersion = System.getProperty("play.version", "2.6.25")
  def scalaVersion = System.getProperty("scala.version", "2.12.2")
  def crossScalaVersions = Seq(scalaVersion, "2.11.12")
}



object Common {
  def version = "master-SNAPSHOT"  
  def playVersion = System.getProperty("play.version", "2.6.12")
  def scalaVersion = System.getProperty("scala.version", "2.12.6")
  def crossScalaVersions = Seq(scalaVersion, "2.11.12")
}

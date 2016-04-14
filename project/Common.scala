

object Common {
  def version = "master-SNAPSHOT"

  def playVersion = System.getProperty("play.version", "2.5.1")

  def scalaVersion = System.getProperty("scala.version", "2.11.8")
}

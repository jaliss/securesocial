import sbt._
import Keys._

object Common {
  def version = "0.1"
  def playVersion = System.getProperty("play.version", "2.3.7")
  def scalaVersion =  System.getProperty("scala.version", "2.11.4")
}

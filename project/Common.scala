import sbt._
import Keys._

object Common {
  def version = "3.0-M1"
  def playVersion = System.getProperty("play.version", "2.3.1")
  def scalaVersion =  System.getProperty("scala.version", "2.11.1")
}

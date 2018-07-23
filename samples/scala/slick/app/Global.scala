import play.api._
import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    InitData.create()
  }

}

object InitData {
  def create() = {
    DB.withSession{ implicit session =>
      if (Users.length.run == 0) {
        /*Users ++= Seq(
          User(None, "Leon", "Radley")
        )*/
      }
    }
  }
}
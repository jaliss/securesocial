package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.libs.json.Json
import securesocial.core._
import json._

object Application extends Controller with SecureSocial {

  def index = DBAction { implicit session =>
    val allUsers = Users.list

    Ok(Json.toJson(allUsers))
  }

  def secured = SecuredAction { implicit request =>
    Ok(Json.toJson(request.user))
  }

}
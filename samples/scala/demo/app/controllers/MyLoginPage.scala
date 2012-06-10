package controllers

import play.api.mvc.{Action}
import securesocial.controllers.BaseLoginPage


object MyLoginPage extends BaseLoginPage
{
    /**
   * Renders the login page
   * @return
   */
  def login = Action { implicit request =>
    Ok(views.html.login(getProviders()))
  }
  
}
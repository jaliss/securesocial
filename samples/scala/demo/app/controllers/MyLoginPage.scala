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
  
  /**
   * Logs out the user by clearing the credentials from the session.
   * The browser is redirected either to the login page or to the page specified in the onLogoutGoTo property.
   *
   * @return
   */
  def logout = Action { implicit request =>
    val to = getLoginUrl(request)
    cleanSession(session)
    Redirect(to)
  }
}
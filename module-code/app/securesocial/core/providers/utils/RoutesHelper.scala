/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.core.providers.utils

import play.api.mvc.Call
import play.api.Play
import play.Logger
import securesocial.controllers.ProviderController

/**
 *
 */
object RoutesHelper {

  // ProviderController
  val pc = Class.forName("securesocial.controllers.ReverseProviderController")
  val providerControllerMethods = pc.newInstance().asInstanceOf[{
    def authenticateByPost(p: String): Call
    def authenticate(p: String): Call
    def notAuthorized: Call
  }]

  def authenticateByPost(provider:String): Call = providerControllerMethods.authenticateByPost(provider)
  def authenticate(provider:String): Call = providerControllerMethods.authenticate(provider)
  def notAuthorized: Call = providerControllerMethods.notAuthorized

  // LoginPage
  val lp = Class.forName("securesocial.controllers.ReverseLoginPage")
  val loginPageMethods = lp.newInstance().asInstanceOf[{
    def logout(): Call
    def login(): Call
  }]

  def login() = loginPageMethods.login()
  def logout() = loginPageMethods.logout()


  ///
  val rr = Class.forName("securesocial.controllers.ReverseRegistration")
  val registrationMethods = rr.newInstance().asInstanceOf[{
    def handleStartResetPassword(): Call
    def handleStartSignUp(): Call
    def handleSignUp(token:String): Call
    def startSignUp(): Call
    def resetPassword(token:String): Call
    def startResetPassword(): Call
    def signUp(token:String): Call
    def handleResetPassword(token:String): Call
  }]

  def handleStartResetPassword() = registrationMethods.handleStartResetPassword()
  def handleStartSignUp() = registrationMethods.handleStartSignUp()
  def handleSignUp(token:String) = registrationMethods.handleSignUp(token)
  def startSignUp() = registrationMethods.startSignUp()
  def resetPassword(token:String) = registrationMethods.resetPassword(token)
  def startResetPassword() = registrationMethods.startResetPassword()
  def signUp(token:String) = registrationMethods.signUp(token)
  def handleResetPassword(token:String) = registrationMethods.handleResetPassword(token)

  ////
  var passChange = Class.forName("securesocial.controllers.ReversePasswordChange")
  val passwordChangeMethods = passChange.newInstance().asInstanceOf[{
    def page(): Call
    def handlePasswordChange(): Call
  }]

  def changePasswordPage() = passwordChangeMethods.page()
  def handlePasswordChange() = passwordChangeMethods.handlePasswordChange()

  val assets = {
    val conf = Play.current.configuration
    val clazz = conf.getString("securesocial.assetsController").getOrElse("controllers.ReverseAssets")
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] assets controller = %s".format(clazz))
    }
    Class.forName(clazz)
  }

  val assetsControllerMethods = assets.newInstance().asInstanceOf[{
    def at(file: String): Call
  }]

  def at(file: String) = assetsControllerMethods.at(file)


  val defaultBootstrapCssPath = "securesocial/bootstrap/css/bootstrap.min.css"
  /**
   * Loads the Bootstrap Css to use from configuration, using a default one if not provided
   * @return the path to Bootstrap css file to use
   */
  val bootstrapCssPath = {
    val conf = Play.current.configuration
    val bsPath = conf.getString("securesocial.bootstrapCssPath").getOrElse(defaultBootstrapCssPath)
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] bootstrap path = %s".format(bsPath))
    }
    at(bsPath)
  }

  val defaultFaviconPath = "securesocial/images/favicon.png"
  /**
   * Loads the Favicon to use from configuration, using a default one if not provided
   * @return the path to Favicon file to use
   */
  val faviconPath = {
    val conf = Play.current.configuration
    val favPath = conf.getString("securesocial.faviconPath").getOrElse(defaultFaviconPath)
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] favicon path = %s".format(favPath))
    }
    at(favPath)
  }

  val defaultJqueryPath = "securesocial/javascripts/jquery-1.7.1.min.js"
  /**
   * Loads the Jquery file to use from configuration, using a default one if not provided
   * @return the path to Jquery file to use
   */
  val jqueryPath = {
    val conf = Play.current.configuration
    val jqueryPath = conf.getString("securesocial.jqueryPath").getOrElse(defaultJqueryPath)
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] Jquery path = %s".format(jqueryPath))
    }
    at(jqueryPath)
  }

  /**
   * Loads the Custom Css file to use from configuration. If there is none define, none will be used
   * @return Option containing a custom css file or None
   */
  val customCssPath: Option[Call] = {
    val conf = Play.current.configuration
    val customPath = conf.getString("securesocial.customCssPath") match {
      case Some(path) => Some(at(path))
      case _ => None
    }
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] custom css path = %s".format(customPath))
    }
    customPath
  }

  /**
   * Sets the path to use when leaving the login page via the "Cancel" button at the bottom.
   * If not set it redirects to "/"
   * @return the path to Jquery file to use
   */
  val cancelLoginPath = {
    val conf = Play.current.configuration
    val cancelLoginPath = conf.getString("securesocial.onCancelLoginGoTo").getOrElse("/")
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] Cancel Login path = %s".format(cancelLoginPath))
    }
    cancelLoginPath
  }

}

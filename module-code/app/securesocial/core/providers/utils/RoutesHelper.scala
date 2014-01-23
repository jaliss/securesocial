/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
import play.Play
import play.Logger
import scala.language.reflectiveCalls

/**
 *
 */
object RoutesHelper {

  lazy val conf = play.api.Play.current.configuration

  // ProviderController
  lazy val pc = Play.application().classloader().loadClass("securesocial.controllers.ReverseProviderController")
  lazy val providerControllerMethods = pc.newInstance().asInstanceOf[{
    def authenticateByPost(p: String): Call
    def authenticate(p: String): Call
    def notAuthorized: Call
  }]

  def authenticateByPost(provider:String): Call = providerControllerMethods.authenticateByPost(provider)
  def authenticate(provider:String): Call = providerControllerMethods.authenticate(provider)
  def notAuthorized: Call = providerControllerMethods.notAuthorized

  // LoginPage
  lazy val lp = Play.application().classloader().loadClass("securesocial.controllers.ReverseLoginPage")
  lazy val loginPageMethods = lp.newInstance().asInstanceOf[{
    def logout(): Call
    def login(): Call
  }]

  def login() = loginPageMethods.login()
  def logout() = loginPageMethods.logout()


  ///
  lazy val rr = Play.application().classloader().loadClass("securesocial.controllers.ReverseRegistration")
  lazy val registrationMethods = rr.newInstance().asInstanceOf[{
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
  lazy val passChange = Play.application().classloader().loadClass("securesocial.controllers.ReversePasswordChange")
  lazy val passwordChangeMethods = passChange.newInstance().asInstanceOf[{
    def page(): Call
    def handlePasswordChange(): Call
  }]

  def changePasswordPage() = passwordChangeMethods.page()
  def handlePasswordChange() = passwordChangeMethods.handlePasswordChange()

  lazy val assets = {
    val clazz = conf.getString("securesocial.assetsController").getOrElse("controllers.ReverseAssets")
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] assets controller = %s".format(clazz))
    }
    Play.application().classloader().loadClass(clazz)
  }

  private lazy val assetsPath = conf.getString("securesocial.assetsPath").getOrElse("/public")
  private type SimpleAt = { def at(file: String): Call }
  private type AtWithPath = { def at(path: String, file: String): Call }
  private case class AtHelper(impl: AtWithPath)  {
    def at(file: String): Call = impl.at(assetsPath, file)
  }

  lazy val assetsControllerMethods: SimpleAt = {
    val instance = assets.newInstance()
    try {
      instance.getClass.getMethod("at", classOf[String], classOf[String])
      AtHelper(instance.asInstanceOf[AtWithPath])
    } catch {
      case e: NoSuchMethodException => instance.asInstanceOf[SimpleAt]
    }
  }

  def at(file: String) = assetsControllerMethods.at(file)


  val defaultBootstrapCssPath = "securesocial/bootstrap/css/bootstrap.min.css"
  /**
   * Loads the Bootstrap Css to use from configuration, using a default one if not provided
   * @return the path to Bootstrap css file to use
   */
  val bootstrapCssPath = {
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
    val customPath = conf.getString("securesocial.customCssPath") match {
      case Some(path) => Some(at(path))
      case _ => None
    }
    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] custom css path = %s".format(customPath))
    }
    customPath
  }

}

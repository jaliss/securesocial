package securesocial.core.providers.utils

import play.api.mvc.Call

/**
 *
 */
object RoutesHelper {
  // ProviderController
  val pc = Class.forName("securesocial.controllers.ReverseProviderController")
  val providerControllerMethods = pc.newInstance().asInstanceOf[{
    def authenticateByPost(p: String): Call
    def authenticate(p: String): Call
  }]

  def authenticateByPost(provider:String): Call = providerControllerMethods.authenticateByPost(provider)
  def authenticate(provider:String): Call = providerControllerMethods.authenticate(provider)

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

  //
  val assets = Class.forName("controllers.ReverseAssets")
  val assetsControllerMethods = assets.newInstance().asInstanceOf[{
    def at(file: String): Call
  }]

  def at(file: String) = assetsControllerMethods.at(file)
}

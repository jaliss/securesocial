import play.api._
import securesocial.core.java.ResolverHandler
import securesocial.core.DefaultResolver
import play.api.mvc.Request

import controllers.routes

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    
		val loginCall = routes.MyLoginPage.login()
		val logoutCall = routes.MyLoginPage.logout()
		
		object MyResolver extends DefaultResolver(loginCall, logoutCall) {

			override def getAuthenticateUrl(provider: String):String = {
				routes.MyLoginPage.authenticate(provider).url
			}

			override def getAuthenticateUrlAbsolute(provider: String,
					req: Request[_]):String = {
				routes.MyLoginPage.authenticate(provider).absoluteURL(
						ResolverHandler.req2req(req))
			}
		}

		ResolverHandler.setResolver(MyResolver)
  }
}
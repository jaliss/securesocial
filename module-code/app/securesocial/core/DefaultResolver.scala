package securesocial.core
import securesocial.core.java.ResolverHandler
import play.api.mvc.Call
import play.api.mvc.Request

abstract class DefaultResolver(loginCall:Call, logoutCall:Call) extends ResolverHandler.Resolver {

	override def getLoginUrl():String = {
		loginCall.url
	}

	override def getLoginUrlAbsolute(req: Request[_]):String = {
		loginCall.absoluteURL(ResolverHandler.req2req(req))
	}

	override def getLogoutUrl():String = {
		logoutCall.url
	}

	override def getLogoutUrlAbsolute(req: Request[_]):String = {
		logoutCall.absoluteURL(ResolverHandler.req2req(req))
	}
	
	override def getAuthenticateUrl(provider: String):String = {
		throw new RuntimeException("You must implement getAuthenticateUrl(String)")
	}

	override def getAuthenticateUrlAbsolute(provider: String,
			req: Request[_]):String = {
		throw new RuntimeException("You must implement getAuthenticateUrlAbsolute(String, play.api.mvc.Request)");
	}
}
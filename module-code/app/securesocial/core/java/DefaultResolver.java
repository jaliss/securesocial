package securesocial.core.java;

import securesocial.core.java.ResolverHandler.Resolver;
import play.mvc.Call;
import play.mvc.Http.Request;

public abstract class DefaultResolver extends Resolver {

	private final play.mvc.Call loginCall;
	private final play.mvc.Call logoutCall;
	
	public DefaultResolver(final Call login, final Call logout) {
		loginCall = login;
		logoutCall = logout;
	}
	@Override
	public String getLoginUrl() {
		return loginCall.url();
	}

	@Override
	public String getLoginUrlAbsolute(final Request req) {
		return loginCall.absoluteURL(req);
	}

	@Override
	public String getLogoutUrl() {
		return logoutCall.url();
	}

	@Override
	public String getLogoutUrlAbsolute(final Request req) {
		return logoutCall.absoluteURL(req);
	}
	
	@Override
	public String getAuthenticateUrl(final String provider) {
		throw new RuntimeException("You must implement getAuthenticateUrl(String)");
	}

	@Override
	public String getAuthenticateUrlAbsolute(final String provider,
			final Request req) {
		throw new RuntimeException("You must implement getAuthenticateUrlAbsolute(String, play.mvc.Http.Request)");
	}

}

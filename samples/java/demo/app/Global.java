import play.*;
import play.mvc.Http.Request;
import play.mvc.Call;
import securesocial.core.java.ResolverHandler;
import securesocial.core.java.DefaultResolver;

import java.util.*;

import controllers.routes;

import models.*;

public class Global extends GlobalSettings {

	public void onStart(Application app) {

		final Call loginCall = routes.MyLoginPage.login();
		final Call logoutCall = routes.MyLoginPage.logout();

		ResolverHandler.setResolver(new DefaultResolver(loginCall, logoutCall) {

			@Override
			public String getAuthenticateUrl(final String provider) {
				return routes.MyLoginPage.authenticate(provider).url();
			}

			@Override
			public String getAuthenticateUrlAbsolute(final String provider,
					final Request req) {
				return routes.MyLoginPage.authenticate(provider).absoluteURL(
						req);
			}
		});
	}

}
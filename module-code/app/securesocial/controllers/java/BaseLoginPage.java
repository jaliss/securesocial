package securesocial.controllers.java;


import java.util.Collection;

import scala.Either;
import play.mvc.Controller;
import play.mvc.Http.Session;
import play.mvc.Result;
import scala.collection.JavaConversions;
import securesocial.controllers.LoginPage;
import securesocial.core.AccessDeniedException;
import securesocial.core.IdentityProvider;
import securesocial.core.ProviderRegistry;
import securesocial.core.SocialUser;
import securesocial.core.java.ResolverHandler;
import securesocial.core.java.SecureSocial;

public abstract class BaseLoginPage extends Controller {
	/**
	 * The property that specifies the page the user is redirected to if there
	 * is no original URL saved in
	 * the session.
	 */
	private static final String onLoginGoTo = LoginPage.onLoginGoTo();

	/**
	 * The property that specifies the page the user is redirected to after
	 * logging out.
	 */
	private static final String onLogoutGoTo = LoginPage.onLogoutGoTo();

	/**
	 * The root path
	 */
	private static final String Root = LoginPage.Root();

	protected static Collection<IdentityProvider> getProviders() {
		return JavaConversions.asJavaCollection(ProviderRegistry.all().values());
	}

	protected static String getLoginUrl(final play.mvc.Http.Request request) {
		String to = play.Play.application().configuration()
				.getString(onLogoutGoTo);
		if (to == null || "".equals(to)) {
			to = ResolverHandler.getResolver().getLoginUrlAbsolute(request);
		}
		return to;
	}

	protected static void cleanSession(final Session session) {
		session.remove(SecureSocial.USER_KEY);
		session.remove(SecureSocial.PROVIDER_KEY);
	}
	
	/**
	 * Logs out the user by clearing the credentials from the session.
	 * The browser is redirected either to the login page or to the page
	 * specified in the onLogoutGoTo property.
	 * 
	 * @return
	 */
	public static Result logout() {
		final String to = getLoginUrl(request());
		cleanSession(session());
		return redirect(to);
	}

	/**
	 * The authentication flow for all providers starts here.
	 * 
	 * @param provider
	 *            The id of the provider that needs to handle the call
	 * @return
	 */
	public static Result authenticate(final String providerId) {

		final scala.Option<IdentityProvider> provider = ProviderRegistry
				.get(providerId);

		if (!provider.isEmpty()) {
//			try {
				final IdentityProvider p = provider.get();
				final play.api.mvc.Request r = ResolverHandler.req2req(session(),request());
				final Either<play.api.mvc.Result, SocialUser> e = p.authenticate(r);
				if (e.isRight()) {
					String to = session().get(SecureSocial.ORIGINAL_URL);
					if (to == null || "".equals(to)) {
						to = play.Play.application().configuration()
								.getString(onLoginGoTo);
					}
					if (to == null || "".equals(to)) {
						to = Root;
					}
					final SocialUser u = e.right().get();
					session(SecureSocial.USER_KEY, u.id().id());
					session(SecureSocial.PROVIDER_KEY, u.id().providerId());
					session().remove(SecureSocial.ORIGINAL_URL);
					return redirect(to);
				} else {
					final play.api.mvc.Result iResult = e.left().get();
					return new Result() {
		                
		                public play.api.mvc.Result getWrappedResult() {
		                    return iResult;
		                }

		                public String toString() {
		                    return iResult.toString();
		                }
		                
		            };
				}
//			} catch (final AccessDeniedException ade) {
//				flash("error", Messages.get("securesocial.login.accessDenied"));
//				return redirect(SecureSocial.getResolver().getLoginCall());
//			}
		}

		return notFound();
	}
}

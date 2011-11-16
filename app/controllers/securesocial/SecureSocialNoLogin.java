package controllers.securesocial;

import play.libs.OAuth;
import play.mvc.Before;
import securesocial.provider.AuthenticationMethod;
import securesocial.provider.IdentityProvider;
import securesocial.provider.OAuth1Provider;
import securesocial.provider.OpenIDOAuthHybridProvider;
import securesocial.provider.ProviderRegistry;
import securesocial.provider.SocialUser;
import securesocial.provider.UserId;
import securesocial.provider.UserService;

public class SecureSocialNoLogin extends SecureSocial {

    /**
     * Checks if there is a user logged in and redirects to the login page if not.
     */
    @Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() throws Throwable
    {
        final UserId userId = getUserId();

    	if ( userId == null ) {
        	renderArgs.put(USER, null);
        } else {
        	SecureSocialLogin.checkAccess();
        }
    }

	
}

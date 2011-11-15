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

public class SecureSocial extends SecureSocialBase {

    /**
     * Checks if there is a user logged in and redirects to the login page if not.
     */
    @Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() throws Throwable
    {
        final UserId userId = getUserId();

        if ( userId == null ) {
            final String originalUrl = request.method.equals(GET) ? request.url : ROOT;
            flash.put(ORIGINAL_URL, originalUrl);
            login();
        } else {
            SocialUser user = UserService.find(userId);
            if ( user == null ) {
                // the user had the cookies but the UserService can't find it ...
                // it must have been erased, redirect to login again.
                clearUserId();
                login();
            }

            // if the user is using OAUTH1 or OPENID HYBRID OAUTH set the ServiceInfo
            // so the app using this module can access it easily to invoke the APIs.
            if ( user.authMethod == AuthenticationMethod.OAUTH1 || user.authMethod == AuthenticationMethod.OPENID_OAUTH_HYBRID ) {
                final OAuth.ServiceInfo sinfo;
                IdentityProvider provider = ProviderRegistry.get(user.id.provider);
                if ( user.authMethod == AuthenticationMethod.OAUTH1 ) {
                    sinfo = ((OAuth1Provider)provider).getServiceInfo();
                } else {
                    sinfo = ((OpenIDOAuthHybridProvider)provider).getServiceInfo();
                }
                user.serviceInfo = sinfo;
            }
            // make the user available in templates
            renderArgs.put(USER, user);
        }
    }

	
}

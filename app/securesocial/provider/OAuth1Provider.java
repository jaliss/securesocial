/**
* Copyright 2011 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.provider;

import play.Play;
import play.cache.Cache;
import play.libs.OAuth;
import play.mvc.Scope;
import play.mvc.results.Redirect;

import java.util.Map;

/**
 * A provider that handles the OAuth1 authentication flow
 */
public abstract class OAuth1Provider extends IdentityProvider {

    private static final String DENIED = "denied";
    private static final String SECURESOCIAL = "securesocial.";
    private OAuth.ServiceInfo serviceInfo;
    private static final String REQUEST_TOKEN_URL = "requestTokenURL";
    private static final String ACCESS_TOKEN_URL = "accessTokenURL";
    private static final String AUTHORIZATION_URL = "authorizationURL";
    private static final String CONSUMER_KEY = "consumerKey";
    private static final String CONSUMER_SECRET = "consumerSecret";


    protected OAuth1Provider(ProviderType type)
    {
        super(type, AuthenticationMethod.OAUTH1);
        serviceInfo = createServiceInfo(getPropertiesKey(type));
    }

    /**
     * Gets the service info required to invoke the APIs for this provider.
     *
     * @return A play.libs.OAuth.ServiceInfo object
     */
    public OAuth.ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    /**
     * Creates a ServiceInfo using the properties in the application.conf
     *
     * @param key The key for this provider
     * @return A OAuth.ServiceInfo object
     */
    public static OAuth.ServiceInfo createServiceInfo(String key) {
        return new OAuth.ServiceInfo(
                Play.configuration.getProperty(key + REQUEST_TOKEN_URL),
                Play.configuration.getProperty(key + ACCESS_TOKEN_URL),
                Play.configuration.getProperty(key + AUTHORIZATION_URL),
                Play.configuration.getProperty(key + CONSUMER_KEY),
                Play.configuration.getProperty(key + CONSUMER_SECRET)
        );

    }

    @Override
    public SocialUser doAuth(Map<String, Object> authContext) throws AccessDeniedException {
        Scope.Params params = Scope.Params.current();

        if ( params.get(DENIED) != null ) {
            throw new AccessDeniedException();
        }

        final String key = new StringBuilder(SECURESOCIAL).append(Scope.Session.current().getId()).toString();
        OAuth service = OAuth.service(serviceInfo);

        if ( !OAuth.isVerifierResponse() ) {
            // first step on the authentication process
            OAuth.Response response = service.retrieveRequestToken();
            if ( response.error != null ) {
                // there was an error retrieving the access token
                throw new AuthenticationException(response.error);
            }
            SocialUser user = createUser();
            user.token = response.token;
            user.secret = response.secret;
            Cache.add(key, user);
            throw new Redirect( service.redirectUrl(response.token), false);
        }

        // the OAuth provider is redirecting back to us
        SocialUser user = (SocialUser) Cache.get(key);
        if ( user == null ) {
            throw new AuthenticationException();
        }
        Cache.delete(key);
        OAuth.Response response = service.retrieveAccessToken(user.token, user.secret);
        if ( response.error != null ) {
            throw new AuthenticationException(response.error);
        }

        // all was ok, replace the tokens
        user.token = response.token;
        user.secret = response.secret;
        user.serviceInfo = serviceInfo;
        return user;
    }
}

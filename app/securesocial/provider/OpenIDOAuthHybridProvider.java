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

import play.Logger;
import play.Play;
import play.libs.OAuth;
import play.libs.OpenID;
import play.libs.WS;
import play.mvc.Scope;
import play.mvc.results.Redirect;

import java.util.Map;

/**
 * Base class for all providers that need to support the OpenID + OAuth Hybrid protocol
 */
public abstract class OpenIDOAuthHybridProvider extends OpenIDProvider
{
    private OAuth.ServiceInfo sinfo;
    private String[][] oauthParameters;
    private static final String OPENID_EXT2_SCOPE = "openid.ext2.scope";
    private static final String OPENID_NS_EXT2 = "openid.ns.ext2";
    private static final String HTTP_SPECS_OPENID_NET_EXTENSIONS_OAUTH_1_0 = "http://specs.openid.net/extensions/oauth/1.0";
    private static final String OPENID_EXT2_CONSUMER = "openid.ext2.consumer";
    private static final String OPENID_OAUTH_REQUEST_TOKEN = "openid.oauth.request_token";
    private static final String OPENID_EXT2_REQUEST_TOKEN = "openid.ext2.request_token";
    private static final String AMPERSAND = "&";
    private static final String EQUALS = "=";
    private static final String EMPTY_SECRET = "";

    protected OpenIDOAuthHybridProvider(ProviderType type, String userFormat) {
        super(type, userFormat);
        final String key = getPropertiesKey(type);
        sinfo = OAuth1Provider.createServiceInfo(key);

        final String scope[] = OAuth2Provider.getScope(key, OPENID_EXT2_SCOPE);
        if ( scope != null ) {
            oauthParameters = new String[3][2];
            oauthParameters[2][0] = scope[0];
            oauthParameters[2][1] = scope[1];

        } else {
            oauthParameters = new String[2][2];
        }
        oauthParameters[0] = new String[] {OPENID_NS_EXT2, HTTP_SPECS_OPENID_NET_EXTENSIONS_OAUTH_1_0};
        oauthParameters[1] = new String[] {OPENID_EXT2_CONSUMER, sinfo.consumerKey};

    }

    /**
     * Returns the ServiceInfo needed to invoke APIs in the service this provider represents
     *
     *
     * @return A OAuth.ServiceInfo object
     */
    public OAuth.ServiceInfo getServiceInfo() {
        return sinfo;
    }

    /**
     * Executes the OpenID + OAuth1 hybrid flow.
     *
     * @see OpenIDProvider#doAuth(java.util.Map) 
     * @see IdentityProvider#doAuth(java.util.Map)
     */
    protected SocialUser doAuth(Map<String, Object> authContext) {
        SocialUser user;

        try {
         user = super.doAuth(authContext);
        } catch ( Redirect redirect ) {
            // todo: add this behaviour into Play's OpenID class.
            redirect.url = addParameters(new StringBuilder(redirect.url), oauthParameters).toString();
            throw redirect;
        }

        // OpenID flow is done, complete the OAuth part        
        Scope.Params params = Scope.Params.current();
        String token = params.get(OPENID_OAUTH_REQUEST_TOKEN);

        if ( token == null ) {
            token = params.get(OPENID_EXT2_REQUEST_TOKEN);
        }
        if ( token == null ) {
            Logger.error("Request token is missing in OpenID+OAuth callback.  Provider: " + type);
            throw new AuthenticationException();
        }
        if ( Logger.isDebugEnabled() ) {
            Logger.debug("openid.ext2.scope = " + params.get(OPENID_EXT2_SCOPE));
            Logger.debug("openid.ext2.request_token = " + token);
        }
        OAuth oauth = OAuth.service(sinfo);
        OAuth.Response response = oauth.retrieveAccessToken(token, EMPTY_SECRET);
        if ( response.error != null ) {
            Logger.error("Error retrieving access token from %s, : %s", type, response.error.toString());
            throw new AuthenticationException(response.error);
        }

        user.token = response.token;
        user.secret = response.secret;
        user.serviceInfo = sinfo;

        if ( Logger.isDebugEnabled() ) {
            Logger.debug("After OAuth exchange: request token = " + token + " -> token = " + user.token + " - secret = " + user.secret);
        }
        user.authMethod = AuthenticationMethod.OPENID_OAUTH_HYBRID;
        return user;
    }

    // a helper method to add parameters to some urls ...
    // todo: fix play so this is not needed.
    private StringBuilder addParameters(StringBuilder url, String [][]additionalParams) {
        // this is a hack, retrieveVerificationCode should allow passing parameters
        if ( additionalParams != null ) {
            for ( int i = 0 ; i < additionalParams.length ; i++ ) {
                url.append(AMPERSAND).append(additionalParams[i][0]).append(EQUALS).append( WS.encode(additionalParams[i][1]));
            }
        }
        return url;
    }
}

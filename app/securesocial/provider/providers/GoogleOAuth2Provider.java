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
package securesocial.provider.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import play.Logger;
import play.Play;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.Scope;
import securesocial.libs.ExtOAuth2;
import securesocial.provider.AuthenticationException;
import securesocial.provider.AuthenticationMethod;
import securesocial.provider.IdentityProvider;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;

import java.util.Map;

/**
 * A Google Provider implementing Oauth2. See https://developers.google.com/accounts/docs/OAuth2
 * In a single flow the user gets authenticated and a token that can be used to invoke
 * Google's APIs is retrieved.
 * see https://developers.google.com/accounts/docs/OAuth2WebServer for details.
 * Available configuration parameters are :
 * <ul>
 * <li>securesocial.googleoauth2.authorizationURL</li>
 * <li>securesocial.googleoauth2.accessTokenURL</li>
 * <li>securesocial.googleoauth2.clientid</li>
 * <li>securesocial.googleoauth2.secret</li>
 * <li>securesocial.googleoauth2.scope</li>
 * <li>securesocial.googleoauth2.access_type : online or offline</li>
 * </ul>
 */
public class GoogleOAuth2Provider extends IdentityProvider {
    private static final String ME_API = "https://www.googleapis.com/oauth2/v1/userinfo?access_token=%s";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String PICTURE = "picture";
    private static final String EMAIL = "email";

    // Configuration propreties keys
    private static final String SCOPE = "scope";
    private static final String AUTHORIZATION_URL = "authorizationURL";
    private static final String ACCESS_TOKEN_URL = "accessTokenURL";
    private static final String CLIENTID = "clientid";
    private static final String SECRET = "secret";
    private static final String ACCESS_TYPE = "access_type";

    private ExtOAuth2 service;


    public GoogleOAuth2Provider() {
        super(ProviderType.googleoauth2, AuthenticationMethod.OAUTH2);

        this.service = createOAuth2Service();
    }

    private ExtOAuth2 createOAuth2Service() {
        String key = getPropertiesKey(type);
        ExtOAuth2 service = new ExtOAuth2(
                Play.configuration.getProperty(key + AUTHORIZATION_URL),
                Play.configuration.getProperty(key + ACCESS_TOKEN_URL),
                Play.configuration.getProperty(key + CLIENTID),
                Play.configuration.getProperty(key + SECRET)
        );
        String scope = Play.configuration.getProperty(key + SCOPE);
        Logger.debug("### Scope is %s", scope);
        if (scope != null && scope.trim().length() > 0) {
            service.addAuthorizationURLExtraParam(SCOPE, scope);
        }
        String accessType = Play.configuration.getProperty(key + ACCESS_TYPE);
        if (accessType != null && accessType.trim().length() > 0) {
            service.addAuthorizationURLExtraParam(ACCESS_TYPE, accessType);
        }
        service.addAuthorizationURLExtraParam("response_type", "code");
        // Google wants the accessToken retrieval to be a POST.
        service.accessTokenURLMethod = ExtOAuth2.POST_METHOD;

        return service;
    }

    /**
     * @return
     * @see IdentityProvider#doAuth(java.util.Map)
     */
    @Override
    protected SocialUser doAuth(Map<String, Object> authContext) {
        Scope.Params params = Scope.Params.current();

        if (params.get(ERROR) != null) {
            // todo: improve this.  Get details of the error and include them in the exception.
            throw new AuthenticationException();
        }

        if (!OAuth2.isCodeResponse()) {
            service.retrieveVerificationCode(getFullUrl());
        }

        final String authUrl = getFullUrl();
        OAuth2.Response response = service.retrieveAccessToken(authUrl);
        if (response == null) {
            throw new AuthenticationException();
        }

        String accessToken = service.extractAccessToken(response);
        accessToken = accessToken == null ? response.accessToken : accessToken;
        if (accessToken == null) {
            // todo: add error to the exception
            throw new AuthenticationException();
        }

        SocialUser user = createUser();
        user.accessToken = accessToken;
        return user;
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        JsonObject me = WS.url(ME_API, user.accessToken).get().getJson().getAsJsonObject();
        JsonObject error = me.getAsJsonObject(ERROR);

        if (error != null) {
            final String message = error.get(MESSAGE).getAsString();
            final String type = error.get(TYPE).getAsString();
            Logger.error("Error retrieving profile information from Facebook. Error type: %s, message: %s.", type, message);
            throw new AuthenticationException();
        }


        user.id.id = me.get(ID).getAsString();
        user.displayName = me.get(NAME).getAsString();
        user.avatarUrl = me.get(PICTURE).getAsString();
        user.email = me.get(EMAIL).getAsString();
    }
}

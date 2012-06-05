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
import play.libs.OpenID;
import play.libs.WS;
import securesocial.provider.*;

import java.util.Map;

/**
 * A Google provider that implements OpenID 2 + OAuth extensions.
 * In a single flow the user gets authenticated and a token that can be used to invoke
 * Google's APIs is retrieved.
 */
public class GoogleProvider extends OpenIDOAuthHybridProvider {
    static final String USER_FORMAT = "https://www.google.com/accounts/o8/id";
    static final String EMAIL = "email";
    static final String FIRST_NAME = "firstName";
    static final String LAST_NAME = "lastName";
    static final String HTTP_AXSCHEMA_ORG_CONTACT_EMAIL = "http://axschema.org/contact/email";
    static final String HTTP_AXSCHEMA_ORG_NAME_PERSON_FIRST = "http://axschema.org/namePerson/first";
    static final String HTTP_AXSCHEMA_ORG_NAME_PERSON_LAST = "http://axschema.org/namePerson/last";
    private static final String SELF_API = "https://www-opensocial.googleusercontent.com/api/people/@me/@self";
    private static final String ENTRY = "entry";
    private static final String THUMBNAIL_URL = "thumbnailUrl";

    public GoogleProvider() {
        super(ProviderType.google, USER_FORMAT);
        authMethod = AuthenticationMethod.OPENID_OAUTH_HYBRID;
    }

    @Override
    protected void configure(OpenID openId) {
        openId.required(EMAIL, HTTP_AXSCHEMA_ORG_CONTACT_EMAIL);
        openId.required(FIRST_NAME, HTTP_AXSCHEMA_ORG_NAME_PERSON_FIRST);
        openId.required(LAST_NAME, HTTP_AXSCHEMA_ORG_NAME_PERSON_LAST);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        OpenID.UserInfo info = (OpenID.UserInfo) authContext.get(OpenIDProvider.USER_INFO);
        user.displayName = FoursquareProvider.fullName(info.extensions.get(FIRST_NAME),info.extensions.get(LAST_NAME));
        user.email = info.extensions.get(EMAIL);

        WS.HttpResponse response = WS.url(SELF_API).oauth(user.serviceInfo, user.token, user.secret).get();
        if ( response.getStatus() != 200 ) {
            // Amazingly, if there's an error Google replies with an html page ... if it were json I could
            // log some info.
            throw new AuthenticationException();
        }
        JsonElement contactInfo = response.getJson();
        JsonElement avatar = contactInfo.getAsJsonObject().getAsJsonObject(ENTRY).get(THUMBNAIL_URL);
        if ( avatar != null ) {
            user.avatarUrl = avatar.getAsString();
        }
    }
}

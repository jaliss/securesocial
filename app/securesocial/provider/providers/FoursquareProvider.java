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
import play.libs.WS;
import securesocial.provider.*;

import java.util.Map;

/**
 * A provider for Foursquare 
 */
public class FoursquareProvider extends OAuth2Provider {
    private static final String SELF_API = "https://api.foursquare.com/v2/users/self?oauth_token=%s";
    private static final String META = "meta";
    private static final String CODE = "code";
    private static final String ERROR_TYPE = "errorType";
    private static final String ERROR_DETAIL = "errorDetail";
    private static final String RESPONSE = "response";
    private static final String USER = "user";
    private static final String ID = "id";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String SPACE = " ";
    private static final String PHOTO = "photo";
    private static final String CONTACT = "contact";
    private static final String EMAIL = "email";

    public FoursquareProvider() {
        super(ProviderType.foursquare);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        JsonObject me = WS.url(SELF_API, user.accessToken).get().getJson().getAsJsonObject();
        JsonObject meta = me.getAsJsonObject(META);

        if ( meta.get(CODE).getAsInt() != 200 ) {
            final String errorType = meta.get(ERROR_TYPE).getAsString();
            final String errorDetail = meta.get(ERROR_DETAIL).getAsString();
            Logger.error("Error retrieving profile information from Foursquare. Error type: %s, detail: %s.", errorType, errorDetail);
            throw new AuthenticationException();
        }

        JsonObject response = me.getAsJsonObject(RESPONSE);
        if ( response == null ) {
            throw new AuthenticationException();
        }

        JsonObject userInfo = response.getAsJsonObject(USER);
        if( userInfo == null ) {
            throw new AuthenticationException();
        }

        user.id.id = userInfo.get(ID).getAsString();
        user.displayName = userInfo.get(FIRST_NAME).getAsString();
        final JsonElement lastName = userInfo.get(LAST_NAME);
        if ( lastName != null ) {
            user.displayName = fullName(user.displayName, lastName.getAsString());
        }
        user.avatarUrl = userInfo.get(PHOTO).getAsString();
        final JsonObject contact = userInfo.getAsJsonObject(CONTACT);
        if ( contact != null ) {
            final JsonElement userEmail = contact.get(EMAIL);
            if ( userEmail != null ) {
                user.email = userEmail.getAsString();
            }
        }
    }

    static String fullName(String first, String last) {
        return new StringBuilder(first).append(SPACE).append(last).toString();
    }
}

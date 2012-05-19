/**
 * Copyright 2011 Brian Porter (brian at porter dot net) - twitter: @poornerd
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
 * A provider for Instagram 
 */
public class InstagramProvider extends OAuth2Provider {
    private static final String SELF_API = "https://api.instagram.com/v1/users/self/?access_token=%s";
    private static final String META = "meta";
    private static final String CODE = "code";
    private static final String ERROR_TYPE = "errorType";
    private static final String ERROR_DETAIL = "errorDetail";
    private static final String USER = "data";
    private static final String ID = "id";
    private static final String FULL_NAME = "full_name";
    private static final String SPACE = " ";
    private static final String PHOTO = "profile_picture";
    private static final String CONTACT = "contact";
    private static final String EMAIL = "email";

    public InstagramProvider() {
        super(ProviderType.instagram);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        JsonObject me = WS.url(SELF_API, user.accessToken).get().getJson().getAsJsonObject();
        JsonObject meta = me.getAsJsonObject(META);

        if ( meta.get(CODE).getAsInt() != 200 ) {
            final String errorType = meta.get(ERROR_TYPE).getAsString();
            final String errorDetail = meta.get(ERROR_DETAIL).getAsString();
            Logger.error("Error retrieving profile information from Instagram. Error type: %s, detail: %s.", errorType, errorDetail);
            throw new AuthenticationException();
        }


        JsonObject userInfo = me.getAsJsonObject(USER);
        if( userInfo == null ) {
            final String errorType = userInfo.get(ERROR_TYPE).getAsString();
            final String errorDetail = userInfo.get(ERROR_DETAIL).getAsString();
            Logger.error("Error retrieving profile data from Instagram. Error type: %s, detail: %s.", errorType, errorDetail);
            throw new AuthenticationException();
        }

        user.id.id = userInfo.get(ID).getAsString();
        user.displayName = userInfo.get(FULL_NAME).getAsString();
        user.avatarUrl = userInfo.get(PHOTO).getAsString();
        final JsonObject contact = userInfo.getAsJsonObject(CONTACT);
        if ( contact != null ) {
            final JsonElement userEmail = contact.get(EMAIL);
            if ( userEmail != null ) {
                user.email = userEmail.getAsString();
            }
        }
    }

}

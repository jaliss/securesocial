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

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import play.Logger;
import play.libs.WS;
import securesocial.provider.*;

import java.util.Map;

/**
 * A provider for LinkedIn
 */
public class LinkedInProvider extends OAuth1Provider
{
    private static final String ME_API = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,picture-url)?format=json";
    private static final String ERROR_CODE = "errorCode";
    private static final String MESSAGE = "message";
    private static final String REQUEST_ID = "requestId";
    private static final String TIMESTAMP = "timestamp";
    private static final String ID = "id";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String PICTURE_URL = "pictureUrl";

    public LinkedInProvider() {
        super(ProviderType.linkedin);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        JsonObject me = WS.url(ME_API).oauth(user.serviceInfo,user.token, user.secret).get().getJson().getAsJsonObject();

        if ( me.get(ERROR_CODE) != null ) {
            int errorCode = me.get(ERROR_CODE).getAsInt();
            final String message = me.get(MESSAGE).getAsString();
            final String requestId = me.get(REQUEST_ID).getAsString();
            final String timestamp = me.get(TIMESTAMP).getAsString();
            Logger.error("Error retrieving profile information from LinkedIn. Error code: %s, message: %s, requestId: %s, timestamp: %s.",
                    errorCode, message, requestId, timestamp);
            throw new AuthenticationException();
        }
        user.id.id = me.get(ID).getAsString();
        user.displayName = FoursquareProvider.fullName(me.get(FIRST_NAME).getAsString(),me.get(LAST_NAME).getAsString());
        JsonElement picture = me.get(PICTURE_URL);
        if(picture != null) {
            user.avatarUrl = picture.getAsString();
        }
        // can't get the email because the LinkedIn API does not provide it.
        //user.email = ;
    }
}

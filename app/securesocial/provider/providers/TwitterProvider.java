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
import play.Logger;
import play.libs.WS;
import securesocial.provider.*;

import java.util.Map;

/**
 * A Twitter Provider
 */
public class TwitterProvider extends OAuth1Provider
{
    private static final String VERIFY_CREDENTIALS = "https://api.twitter.com/1.1/account/verify_credentials.json";
    private static final String ERROR = "error";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String PROFILE_IMAGE_URL = "profile_image_url";

    public TwitterProvider() {
        super(ProviderType.twitter);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        JsonObject me = WS.url(VERIFY_CREDENTIALS).oauth(user.serviceInfo, user.token, user.secret).get().getJson().getAsJsonObject();

        if ( me.get(ERROR) != null ) {
            Logger.error("Error retrieving profile information from Twitter. Error: %s", me.get(ERROR).getAsString());
            throw new AuthenticationException();
        }
        user.id.id = me.get(ID).getAsString();
        user.displayName = me.get(NAME).getAsString();
        user.avatarUrl = me.get(PROFILE_IMAGE_URL).getAsString();

        // can't get the email because the Twitter API does not provide it.
        //user.email = ;
    }
}

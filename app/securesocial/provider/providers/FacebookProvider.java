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
 * A Facebook Provider
 */
public class FacebookProvider extends OAuth2Provider
{
    private static final String ME_API = "https://graph.facebook.com/me?fields=name,picture,email&access_token=%s";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String PICTURE = "picture";
    private static final String EMAIL = "email";
    public static final String DATA = "data";
    public static final String URL = "url";

    public FacebookProvider() {
        super(ProviderType.facebook);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        JsonObject me = WS.url(ME_API, user.accessToken).get().getJson().getAsJsonObject();
        JsonObject error = me.getAsJsonObject(ERROR);

        if ( error != null ) {
            final String message = error.get(MESSAGE).getAsString();
            final String type = error.get(TYPE).getAsString();
            Logger.error("Error retrieving profile information from Facebook. Error type: %s, message: %s.", type, message);
            throw new AuthenticationException();
        }
        
        user.id.id = me.get(ID).getAsString();
        user.displayName = me.get(NAME).getAsString();

        //
        // Starting October 2012 the picture field will become a json object.
        // making the code compatible with the old and new version for now.
        //
        JsonElement picture = me.get(PICTURE);
        user.avatarUrl = !picture.isJsonObject() ? picture.getAsString() : picture.getAsJsonObject().get(DATA).getAsJsonObject().get(URL).getAsString();
        user.email = me.get(EMAIL).getAsString();
    }
}

/**
 * Copyright 2011 Ealden Esto E. Escanan (ealden at gmail dot com) - twitter: @ealden
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
import play.libs.WS;
import securesocial.provider.OAuth2Provider;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;

import java.util.Map;

public class GitHubProvider extends OAuth2Provider {
    private static final String AUTHENTICATED_USER = "https://api.github.com/user?access_token=%s";

    private static final String LOGIN = "login";
    private static final String NAME = "name";
    private static final String PICTURE = "avatar_url";
    private static final String EMAIL = "email";

    public GitHubProvider() {
        super(ProviderType.github);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        WS.HttpResponse response = WS.url(AUTHENTICATED_USER, user.accessToken).get();
        JsonObject authenticatedUser = response.getJson().getAsJsonObject();

        user.id.id = authenticatedUser.get(LOGIN).getAsString();
        user.displayName = authenticatedUser.get(NAME).getAsString();
        user.avatarUrl = authenticatedUser.get(PICTURE).getAsString();
        user.email = authenticatedUser.get(EMAIL).getAsString();
    }
}

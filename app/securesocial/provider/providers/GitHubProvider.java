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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.libs.WS;
import securesocial.provider.AuthenticationException;
import securesocial.provider.OAuth2Provider;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GitHubProvider extends OAuth2Provider {
    private static final String GITHUB_ORGANIZATION = "securesocial.github.organization";

    private static final String AUTHENTICATED_USER = "https://api.github.com/user?access_token=%s";
    private static final String AUTHENTICATED_USER_ORGS = "https://api.github.com/user/orgs?access_token=%s";

    private static final String LOGIN = "login";
    private static final String NAME = "name";
    private static final String PICTURE = "avatar_url";
    private static final String EMAIL = "email";

    private static final String ERROR_MESSAGE = "message";

    public GitHubProvider() {
        super(ProviderType.github);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        WS.HttpResponse response = WS.url(AUTHENTICATED_USER, user.accessToken).get();

        if (response.success()) {
            populateSocialUserInfo(user, response);
        } else {
            handleError(response);
        }
    }

    private void populateSocialUserInfo(SocialUser user, WS.HttpResponse response) {
        JsonObject authenticatedUser = response.getJson().getAsJsonObject();

        String organization = (String) Play.configuration.get(GITHUB_ORGANIZATION);

        if (StringUtils.isNotBlank(organization)) {
            validateUserMembership(user, organization);
        }

        user.id.id = authenticatedUser.get(LOGIN).getAsString();

        JsonElement displayName = authenticatedUser.get(NAME);

        if (displayName != null) {
            user.displayName = displayName.getAsString();
        } else {
            user.displayName = user.id.id;
        }

        JsonElement picture = authenticatedUser.get(PICTURE);

        if (picture != null) {
            user.avatarUrl = picture.getAsString();
        }

        JsonElement email = authenticatedUser.get(EMAIL);

        if (email != null) {
            user.email = email.getAsString();
        }
    }

    private void handleError(WS.HttpResponse response) {
        Integer status = response.getStatus();

        JsonObject error = response.getJson().getAsJsonObject();
        String message = error.get(ERROR_MESSAGE).getAsString();

        Logger.error("Error retrieving user information. Status: %i, message: %s.", status, message);

        throw new AuthenticationException();
    }

    private void validateUserMembership(SocialUser user, String organization) {
        WS.HttpResponse response = WS.url(AUTHENTICATED_USER_ORGS, user.accessToken).get();

        if (response.success()) {
            validateUserMembership(organization, response);
        } else {
            handleError(response);
        }
    }

    private void validateUserMembership(String organization, WS.HttpResponse response) {
        JsonArray userOrganizations = response.getJson().getAsJsonArray();
        List<String> organizations = extractOrganizations(userOrganizations);

        if (!organizations.contains(organization)) {
            Logger.error("User not part of, or user membership not public for, organization: %s.", organization);

            throw new AuthenticationException();
        }
    }

    private List<String> extractOrganizations(JsonArray userOrganizations) {
        List<String> organizations = new ArrayList<String>();

        Iterator<JsonElement> i = userOrganizations.iterator();

        while (i.hasNext()) {
            JsonObject userOrganization = i.next().getAsJsonObject();
            String organization = userOrganization.get(LOGIN).getAsString();

            organizations.add(organization);
        }

        return organizations;
    }
}

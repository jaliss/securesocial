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

import play.libs.OAuth;

import java.util.Date;

/**
 * A class representing a conected user and its authentication details.
 */
public class SocialUser implements java.io.Serializable {
    /**
     * The user id
     */
    public UserId id;

    /**
     * The user full name.
     */
    public String displayName;

    /**
     * The user's email
     */
    public String email;

    /**
     * A URL pointing to an avatar
     */
    public String avatarUrl;

    /**
     * The time of the last login.  This is set by the SecureSocial controller.
     */
    public Date lastAccess;

    /**
     * The method that was used to authenticate the user.
     */
    public AuthenticationMethod authMethod;

    /**
     * The service info required to make calls to the API for OAUTH1 users
     * (available when authMethod is OAUTH1 or OPENID_OAUTH_HYBRID)
     *
     * Note: this value does not need to be persisted by UserService since it is set automatically
     * in the SecureSocial Controller for each request that needs it.
     */
    public OAuth.ServiceInfo serviceInfo;

    /**
     * The OAuth1 token (available when authMethod is OAUTH1 or OPENID_OAUTH_HYBRID)
     */
    public String token;

    /**
     * The OAuth1 secret (available when authMethod is OAUTH1 or OPENID_OAUTH_HYBRID)
     */
    public String secret;

    /**
     * The OAuth2 access token (available when authMethod is OAUTH2)
     */
    public String accessToken;

    /**
     * The user password (available when authMethod is USER_PASSWORD)
     */
    public String password;

    /**
     * A boolean indicating if the user has validated his email adddress (available when authMethod is USER_PASSWORD)
     */
    public boolean isEmailVerified;
}

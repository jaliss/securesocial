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
 *
 */
package securesocial.provider.providers;

import play.libs.OpenID;
import play.libs.WS;
import securesocial.provider.OpenIDProvider;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * A Wordpress Provider
 *
 */
public class WordpressProvider extends OpenIDProvider
{
    private static final String USER_FORMAT = "http://{username}.wordpress.com";
    private static final String EMAIL = "email";
    private static final String FULLNAME = "fullname";
    private static final String GRAVATAR_URL = "http://www.gravatar.com/avatar/";
    private static final String D_404 = "?d=404";
    private static final String MD5 = "MD5";

    public WordpressProvider() {
        super(ProviderType.wordpress, USER_FORMAT);
    }
    

    @Override
    protected void configure(OpenID openId) {
        openId.required(EMAIL);
        openId.required(FULLNAME);

    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        OpenID.UserInfo me = (OpenID.UserInfo) authContext.get(OpenIDProvider.USER_INFO);
        user.displayName = me.extensions.get(FULLNAME);
        user.email = me.extensions.get(EMAIL);

        String hash = gravatarHash(user.email);
        if ( hash != null ) {
            StringBuilder sb = new StringBuilder(GRAVATAR_URL).append(hash);
            String gravatar = sb.toString();
            WS.HttpResponse response = WS.url(sb.append(D_404).toString()).get();
            if ( response.success() ) {
                user.avatarUrl = gravatar;
            }
        }
    }

    private String gravatarHash(String email) {
        String result = null;

        email = email.trim().toLowerCase();
        if ( email.length() > 0 ) {
            try {
                MessageDigest m = MessageDigest.getInstance(MD5);
                byte[] out = m.digest(email.getBytes());
                result = new BigInteger(1, out).toString(16);
            } catch (NoSuchAlgorithmException e) {
                // ignore
            }
        }

        return result;
    }
}

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

import play.libs.OpenID;
import securesocial.provider.OpenIDOAuthHybridProvider;
import securesocial.provider.OpenIDProvider;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;

import java.util.Map;

/**
 * An Openid provider for Yahoo
 */
public class YahooProvider extends OpenIDOAuthHybridProvider
{
    private static final String USER_FORMAT = "https://me.yahoo.com";
    private static final String EMAIL = "email";
    private static final String FULLNAME = "fullname";
    private static final String IMAGE = "image";
    private static final String HTTP_AXSCHEMA_ORG_CONTACT_EMAIL = "http://axschema.org/contact/email";
    private static final String HTTP_AXSCHEMA_ORG_NAME_PERSON = "http://axschema.org/namePerson";
    private static final String HTTP_AXSCHEMA_ORG_MEDIA_IMAGE_DEFAULT = "http://axschema.org/media/image/default";

    public YahooProvider() {
        super(ProviderType.yahoo, USER_FORMAT);
    }

    @Override
    protected void configure(OpenID openId) {
        openId.required(EMAIL, HTTP_AXSCHEMA_ORG_CONTACT_EMAIL);
        openId.required(FULLNAME, HTTP_AXSCHEMA_ORG_NAME_PERSON);
        openId.required(IMAGE, HTTP_AXSCHEMA_ORG_MEDIA_IMAGE_DEFAULT);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        OpenID.UserInfo me = (OpenID.UserInfo) authContext.get(OpenIDProvider.USER_INFO);
        user.displayName = me.extensions.get(FULLNAME);
        user.avatarUrl = me.extensions.get(IMAGE);
        user.email = me.extensions.get(EMAIL);
    }
}

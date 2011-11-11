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
import securesocial.provider.OpenIDProvider;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;

import java.util.Map;

/**
 * A provider for MyOpenID
 */
public class MyOpenIDProvider extends OpenIDProvider{
    private static final String USER_FORMAT = "http://{username}.myopenid.com/";
    private static final String FULL_NAME = "fullName";
    private static final String EMAIL = "email";
    private static final String IMAGE = "image";
    private static final String HTTP_SCHEMA_OPENID_NET_NAME_PERSON = "http://schema.openid.net/namePerson";
    private static final String HTTP_SCHEMA_OPENID_NET_CONTACT_EMAIL = "http://schema.openid.net/contact/email";
    private static final String HTTP_SCHEMA_OPENID_NET_MEDIA_IMAGE_DEFAULT = "http://schema.openid.net/media/image/default";

    public MyOpenIDProvider() {
        super(ProviderType.myopenid, USER_FORMAT);
    }

    @Override
    protected void configure(OpenID openId) {
        openId.required(FULL_NAME, HTTP_SCHEMA_OPENID_NET_NAME_PERSON);
        openId.required(EMAIL, HTTP_SCHEMA_OPENID_NET_CONTACT_EMAIL);
        //todo: myopenid is not returning the image ... not supported or wrong type?
        openId.required(IMAGE, HTTP_SCHEMA_OPENID_NET_MEDIA_IMAGE_DEFAULT);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        OpenID.UserInfo me = (OpenID.UserInfo) authContext.get(OpenIDProvider.USER_INFO);
        user.displayName = me.extensions.get(FULL_NAME);
        user.email = me.extensions.get(EMAIL);
        user.avatarUrl = me.extensions.get(IMAGE);
    }
}

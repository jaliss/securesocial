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

import static securesocial.provider.providers.GoogleProvider.*;

import play.libs.OpenID;
import securesocial.provider.*;

import java.util.Map;

/**
 * A Google provider that implements OpenID 2 authentication.
 */
public class GoogleOpenIDProvider extends OpenIDProvider {

    public GoogleOpenIDProvider() {
        super(ProviderType.googleopenid, USER_FORMAT);
    }

    @Override
    protected void configure(OpenID openId) {
        openId.required(EMAIL, HTTP_AXSCHEMA_ORG_CONTACT_EMAIL);
        openId.required(FIRST_NAME, HTTP_AXSCHEMA_ORG_NAME_PERSON_FIRST);
        openId.required(LAST_NAME, HTTP_AXSCHEMA_ORG_NAME_PERSON_LAST);
    }

    @Override
    protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
        OpenID.UserInfo info = (OpenID.UserInfo) authContext.get(OpenIDProvider.USER_INFO);
        user.displayName = FoursquareProvider.fullName(info.extensions.get(FIRST_NAME),info.extensions.get(LAST_NAME));
        user.email = info.extensions.get(EMAIL);
    }

}

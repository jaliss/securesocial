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

import play.i18n.Messages;
import play.libs.OpenID;
import play.mvc.Http;
import play.mvc.Scope;

import java.util.Map;

/**
 * An OpenID provider
 */
public abstract class OpenIDProvider extends IdentityProvider
{
    private boolean needsUsername;
    private String userFormat;
    private static final String USERNAME_TAG = "{username}";
    private static final String OPENID_USER = "openid.user";
    private static final String USERNAME_REGEX = "\\{username\\}";
    protected static final String USER_INFO = "userInfo";
    private static final String SECURESOCIAL_OPEN_ID_USER_NOT_SPECIFIED = "securesocial.openIdUserNotSpecified";

    /**
     * Creates an OpenID provider.
     *
     * @param type the Provider Type (eg: myopenid)
     * @param userFormat The user url format (eg: http://{username}.wordpress.com)
     */
    protected OpenIDProvider(ProviderType type, String userFormat) {
        super(type, AuthenticationMethod.OPENID);
        this.userFormat = userFormat;
        needsUsername = userFormat.indexOf(USERNAME_TAG) != -1;
    }

    /**
     * Returns the user url (eg: http://user.wordpress.com) for an OpenID service.
     * The method checks if the userFormat has a {username} tag and if it does then
     * looks for the username value in the request parameters.
     *
     * If there is no {username} tag the userFormat is used a passed by the subclass.
     * This is because some providers (eg: google or yahoo) do not need the username in the
     * url.
     *  
     * @return The url representing the user.
     */
    protected String getUser() {
        final String user;
        if ( needsUsername ) {
            final String username = Scope.Params.current().get(OPENID_USER);
            if ( username == null || username.trim().length() == 0) {
                Scope.Flash.current().error(Messages.get(SECURESOCIAL_OPEN_ID_USER_NOT_SPECIFIED));
                throw new AuthenticationException();
            }
            user = userFormat.replaceFirst(USERNAME_REGEX, username);
        } else {
            user = userFormat;
        }
        return user;
    }


    /**
     * Returns true if the userFormat does not have a {username} tag.
     * @return  A boolean
     */
    protected boolean needsUsername() {
        return needsUsername;
    }

    /**
     * @see IdentityProvider#doAuth(java.util.Map) 
     */
    @Override
    protected SocialUser doAuth(Map<String, Object> authContext) {
        if ( !OpenID.isAuthenticationResponse() ) {
            OpenID openId = OpenID.id(getUser());
            final String url = getFullUrl();
            openId.returnTo( url );
            openId.forRealm( Http.Request.current().getBase() );
            configure(openId);
            if  ( !openId.verify() ) {
                throw new AuthenticationException();
            }  
        }
        //
        OpenID.UserInfo verifiedUser = OpenID.getVerifiedID();
        if ( verifiedUser == null ) {
            throw new AuthenticationException();
        }
        authContext.put(USER_INFO, verifiedUser);
        SocialUser user = createUser();
        user.id.id = verifiedUser.id;
        return user;
    }

    /**
     * This method allows subclasses to set up additional settings (such as specifying attribute exchange or sreg)
     * before redirecting the user to the OpenId provider.
     *
     * @param openId
     */
    protected abstract void configure(OpenID openId);
}

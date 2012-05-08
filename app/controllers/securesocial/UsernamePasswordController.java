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
package controllers.securesocial;

import notifiers.securesocial.Mails;
import play.Logger;
import play.data.validation.Email;
import play.data.validation.Equals;
import play.data.validation.Required;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Router;
import securesocial.provider.*;
import securesocial.utils.SecureSocialPasswordHasher;

/**
 * The controller for the UI required by the Username Password Provider.
 */
public class UsernamePasswordController extends Controller
{
    static final String SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML = "securesocial/SecureSocial/noticePage.html";

    private static final String USER_NAME = "userName";
    private static final String SECURESOCIAL_USER_NAME_TAKEN = "securesocial.userNameTaken";
    private static final String SECURESOCIAL_ERROR_CREATING_ACCOUNT = "securesocial.errorCreatingAccount";
    private static final String SECURESOCIAL_ACCOUNT_CREATED = "securesocial.accountCreated";
    private static final String SECURESOCIAL_ACTIVATION_TITLE = "securesocial.activationTitle";
    private static final String DISPLAY_NAME = "displayName";
    private static final String EMAIL = "email";
    private static final String SECURESOCIAL_INVALID_LINK = "securesocial.invalidLink";
    private static final String SECURESOCIAL_ACTIVATION_SUCCESS = "securesocial.activationSuccess";
    private static final String SECURESOCIAL_SECURE_SOCIAL_LOGIN = "securesocial.SecureSocial.login";
    private static final String SECURESOCIAL_ACTIVATE_TITLE = "securesocial.activateTitle";

    /**
     * Renders the sign up page.
     */
     public static void signup() {
        render();
    }

    /**
     * Creates an account
     *
     * @param userName      The username
     * @param displayName   The user's full name
     * @param email         The email
     * @param password      The password
     * @param password2     The password verification
     */
    public static void createAccount(@Required(message = "securesocial.required") String userName,
                                     @Required String displayName,
                                     @Required @Email(message = "securesocial.invalidEmail") String email,
                                     @Required String password,
                                     @Required @Equals(message = "securesocial.passwordsMustMatch", value = "password") String password2) {
        if ( validation.hasErrors() ) {
            tryAgain(userName, displayName, email);
        }

        UserId id = new UserId();
        id.id = userName;
        id.provider = ProviderType.userpass;

        if ( UserService.find(id) != null ) {
            validation.addError(USER_NAME, Messages.get(SECURESOCIAL_USER_NAME_TAKEN));
            tryAgain(userName, displayName, email);
        }
        SocialUser user = new SocialUser();
        user.id = id;
        user.displayName = displayName;
        user.email = email;
        user.password = SecureSocialPasswordHasher.passwordHash(password);
        // the user will remain inactive until the email verification is done.
        user.isEmailVerified = false;
        user.authMethod = AuthenticationMethod.USER_PASSWORD;

        try {
            UserService.save(user);
        } catch ( Throwable e ) {
            Logger.error(e, "Error while invoking UserService.save()");
            flash.error(Messages.get(SECURESOCIAL_ERROR_CREATING_ACCOUNT));
            tryAgain(userName, displayName, email);
        }

        // create an activation id
        final String uuid = UserService.createActivation(user);
        Mails.sendActivationEmail(user, uuid);
        flash.success(Messages.get(SECURESOCIAL_ACCOUNT_CREATED));
        final String title = Messages.get(SECURESOCIAL_ACTIVATION_TITLE, user.displayName);
        render(SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
    }

    private static void tryAgain(String username, String displayName, String email) {
        flash.put(USER_NAME, username);
        flash.put(DISPLAY_NAME, displayName);
        flash.put(EMAIL, email);
        validation.keep();
        signup();
    }

    /**
     * The action invoked from the activation email the user receives after signing up.
     *
     * @param uuid The activation id
     */
    public static void activate(String uuid) {
        try {
            if ( UserService.activate(uuid) == false ) {
                flash.error( Messages.get(SECURESOCIAL_INVALID_LINK) );
            } else {
                flash.success(Messages.get(SECURESOCIAL_ACTIVATION_SUCCESS, Router.reverse(SECURESOCIAL_SECURE_SOCIAL_LOGIN)));
            }
        } catch ( Throwable t) {
            Logger.error(t, "Error while activating account");
            flash.error(Messages.get(SECURESOCIAL_ERROR_CREATING_ACCOUNT));
        }
        final String title = Messages.get(SECURESOCIAL_ACTIVATE_TITLE);
        render(SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
    }
}

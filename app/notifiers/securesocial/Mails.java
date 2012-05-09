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
package notifiers.securesocial;

import controllers.securesocial.UsernamePasswordController;
import play.Play;
import play.mvc.Mailer;
import play.mvc.Router;
import securesocial.provider.SocialUser;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to send welcome emails to users that signed up using the
 * Username Password controller
 *
 * @see securesocial.provider.providers.UsernamePasswordProvider
 * @see controllers.securesocial.UsernamePasswordController
 */
public class Mails extends Mailer {
    private static final String SECURESOCIAL_ACTIVATION_MAILER_SUBJECT = "securesocial.mailer.subject";
    private static final String SECURESOCIAL_MAILER_FROM = "securesocial.mailer.from";
    private static final String SECURESOCIAL_USERNAME_PASSWORD_CONTROLLER_ACTIVATE = "securesocial.UsernamePasswordController.activate";

    private static final String SECURESOCIAL_RESET_MAILER_SUBJECT = "securesocial.mailer.reset.subject";
    private static final String SECURESOCIAL_RESET_PASSWORD_CONTROLLER_CHANGE = "securesocial.PasswordResetController.changePassword";

    private static final String UUID = "uuid";
    private static final String USERNAME = "username";

    public static void sendActivationEmail(SocialUser user, String uuid) {
        setSubject(Play.configuration.getProperty(SECURESOCIAL_ACTIVATION_MAILER_SUBJECT));
        setFrom(Play.configuration.getProperty(SECURESOCIAL_MAILER_FROM));
        addRecipient(user.email);
        Map<String, Object> args = new HashMap<String, Object>();
        args.put(UUID, uuid);
        String activationUrl = Router.getFullUrl(SECURESOCIAL_USERNAME_PASSWORD_CONTROLLER_ACTIVATE, args);
        send(user, activationUrl);
    }

    public static void sendPasswordResetEmail(SocialUser user, String uuid) {
        setSubject(Play.configuration.getProperty(SECURESOCIAL_RESET_MAILER_SUBJECT));
        setFrom(Play.configuration.getProperty(SECURESOCIAL_MAILER_FROM));
        addRecipient(user.email);
        Map<String, Object> args = new HashMap<String, Object>();
        args.put(USERNAME, user.id.id);
        args.put(UUID, uuid);
        String activationUrl = Router.getFullUrl(SECURESOCIAL_RESET_PASSWORD_CONTROLLER_CHANGE, args);
        send(user, activationUrl);
    }

}

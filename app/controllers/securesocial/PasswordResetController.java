package controllers.securesocial;

import notifiers.securesocial.Mails;
import play.Logger;
import play.data.validation.Email;
import play.data.validation.Equals;
import play.data.validation.Required;
import play.i18n.Messages;
import play.mvc.Controller;
import securesocial.provider.SocialUser;
import securesocial.provider.UserService;
import securesocial.utils.SecureSocialPasswordHasher;

/**
 * Controller for handling the password reset flow, for cases where the user has lost his/her
 * password and needs a way to reset it to a known one
 */
public class PasswordResetController extends Controller {

    private static final String PASSWORD_IS_RESET = "securesocial.resetSuccess";
    private static final String PASSWORD_RESET_TITLE = "securesocial.resetSuccessTitle";
    private static final String RESET_MAIL_SENT = "securesocial.resetEmailSent";
    private static final String RESET_MAIL_SENT_TITLE = "securesocial.resetEmailSentTitle";

    private static final String INVALID_RESET_TITLE = "securesocial.invalidResetTitle";
    private static final String INVALID_RESET_LINK = "securesocial.invalidResetLink";
    private static final String SECURESOCIAL_ERROR_PASSWORD_RESET = "securesocial.resetError";


    public static void resetPassword() {
        session.getAuthenticityToken();
        render();
    }

    /**
     * Post endpoint for sending out password reset emails
     *
     * @param email
     */
    public static void sendEmail(@Required @Email(message = "securesocial.invalidEmail") String email) {
        checkAuthenticity();
        if (validation.hasErrors()) {
            tryAgainRequestReset(email);
        }

        try {
            // Check that email exists in the database
            SocialUser user = UserService.find(email);

            if (user == null) {
                // Show "email sent" page even if the user does not exist, to prevent figuring out emails this way
                showEmailSuccessPage(email);
            }

            final String uuid = UserService.createPasswordReset(user);
            Mails.sendPasswordResetEmail(user, uuid);
            showEmailSuccessPage(email);
        } catch (Exception e) {
            Logger.error(e, "Error while invoking " + PasswordResetController.class.getSimpleName() + ".sendEmail");
            flash.error(Messages.get(SECURESOCIAL_ERROR_PASSWORD_RESET));
            tryAgainRequestReset(email);
        }
    }

    /**
     * Show a success page for sending out the reset email. This page does double duty as the error page, when
     * a user requests a password reset for an email that we don't know about
     */
    private static void showEmailSuccessPage(final String email) {
        flash.success(Messages.get(RESET_MAIL_SENT, email));
        final String title = Messages.get(RESET_MAIL_SENT_TITLE);
        render(UsernamePasswordController.SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
    }


    /**
     * The provided email
     *
     * @param email
     */
    private static void tryAgainRequestReset(String email) {
        flash.put(SecureSocial.EMAIL, email);
        validation.keep();
        resetPassword();
    }


    /**
     * Controller for rendering the reset my password page
     *
     * @param username
     * @param uuid
     */
    public static void changePassword(String username, String uuid) {
        if (validation.hasErrors()) {
            // Not valid username and uuid -> show error page
            showInvalidLinkFollowedPage();
        }

        try {
            SocialUser user = UserService.fetchForPasswordReset(username, uuid);
            if (user == null) {
                showInvalidLinkFollowedPage();
            }

            renderArgs.put(SecureSocial.USERNAME, username);
            renderArgs.put(SecureSocial.UUID, uuid);
            render();
        } catch (Exception e) {
            Logger.error(e, "Error while invoking " + PasswordResetController.class.getSimpleName() + ".changePassword");
            flash.error(Messages.get(SECURESOCIAL_ERROR_PASSWORD_RESET));
            showInvalidLinkFollowedPage();
        }
    }

    /**
     * Post endpoint for the new password. Requires the username, uuid and authenticity token to be present, in order
     * to allow the password change to continue
     *
     * @param username
     * @param uuid
     * @param newPassword
     * @param confirmPassword
     */
    public static void doChange(@Required String username,
                                @Required String uuid,
                                @Required String newPassword,
                                @Required @Equals(message = "securesocial.passwordsMustMatch", value = "newPassword") String confirmPassword) {
        checkAuthenticity();
        if (validation.hasErrors()) {
            tryAgainChangePassword(username, uuid, newPassword, confirmPassword);
        }

        SocialUser user = UserService.fetchForPasswordReset(username, uuid);
        if (user == null) {
            showInvalidLinkFollowedPage();
        }

        try {
            user.password = SecureSocialPasswordHasher.passwordHash(newPassword);
            UserService.disableResetCode(username, uuid);
            UserService.save(user);

            flash.success(Messages.get(PASSWORD_IS_RESET));
            final String title = Messages.get(PASSWORD_RESET_TITLE);
            render(UsernamePasswordController.SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
        } catch (Exception e) {
            Logger.error(e, "Error while invoking " + PasswordResetController.class.getSimpleName() + ".doChange");
            flash.error(Messages.get(SECURESOCIAL_ERROR_PASSWORD_RESET));
            tryAgainChangePassword(username, uuid, newPassword, confirmPassword);
        }
    }

    /**
     * Show the notification page, with an "invalid link followed"-message
     */
    private static void showInvalidLinkFollowedPage() {
        flash.error(Messages.get(INVALID_RESET_LINK));
        final String title = Messages.get(INVALID_RESET_TITLE);
        render(UsernamePasswordController.SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
    }

    /**
     * Passwords didn't match, so let the user try again
     *
     * @param username
     * @param uuid
     * @param newPassword
     * @param confirmPassword
     */
    private static void tryAgainChangePassword(String username, String uuid, String newPassword, String confirmPassword) {
        flash.put(SecureSocial.NEW_PASSWORD, newPassword);
        flash.put(SecureSocial.CONFIRM_PASSWORD, confirmPassword);
        validation.keep();
        changePassword(username, uuid);
    }


}

package controllers.securesocial;

import play.Logger;
import play.data.validation.Equals;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import securesocial.provider.SocialUser;
import securesocial.provider.UserService;
import securesocial.utils.SecureSocialPasswordHasher;

/**
 * User: kink
 * Date: 2012.05.08
 * Time: 22:22
 */
@With(SecureSocial.class)
public class PasswordChangeController extends Controller {

    protected static final String SECURESOCIAL_ERROR_CHANGING_PASSWORD = "securesocial.changeError";
    protected static final String SECURESOCIAL_CHANGE_CURRENT_PASSWORD_ERROR = "securesocial.changeCurrentPasswordError";
    protected static final String SECURESOCIAL_WRONG_PASSWORD = "securesocial.wrongPassword";
    protected static final String SECURESOCIAL_PASSWORD_CHANGED = "securesocial.changeSuccess";
    protected static final String SECURESOCIAL_PASSWORD_CHANGE_TITLE = "securesocial.changeSuccessTitle";

    public static void changePassword() {
        session.getAuthenticityToken();
        render();
    }

    public static void doChange(@Required String currentPassword,
                                @Required String newPassword,
                                @Required @Equals(message = "securesocial.passwordsMustMatch", value = "newPassword") String confirmPassword) {
        checkAuthenticity();
        if (validation.hasErrors()) {
            tryAgain(newPassword, confirmPassword);
        }

        SocialUser user = SecureSocial.getCurrentUser();
        if (user == null) {
            // Error, as we should be logged in already, and thus have the user available
            error();
        }

        if (!SecureSocialPasswordHasher.verifyPasswordHash(currentPassword, user.password)) {
            flash.error(Messages.get(SECURESOCIAL_CHANGE_CURRENT_PASSWORD_ERROR));
            Validation.addError(SecureSocial.CURRENT_PASSWORD, SECURESOCIAL_WRONG_PASSWORD);

            tryAgain(newPassword, confirmPassword);
        }

        user.password = SecureSocialPasswordHasher.passwordHash(newPassword);

        try {
            UserService.save(user);
        } catch (Throwable e) {
            Logger.error(e, "Error while invoking UserService.save()");
            flash.error(Messages.get(SECURESOCIAL_ERROR_CHANGING_PASSWORD));
            tryAgain(newPassword, confirmPassword);
        }

        flash.success(Messages.get(SECURESOCIAL_PASSWORD_CHANGED));
        final String title = Messages.get(SECURESOCIAL_PASSWORD_CHANGE_TITLE);
        render(UsernamePasswordController.SECURESOCIAL_SECURE_SOCIAL_NOTICE_PAGE_HTML, title);
    }

    private static void tryAgain(String newPassword, String confirmPassword) {
        flash.put(SecureSocial.NEW_PASSWORD, newPassword);
        flash.put(SecureSocial.CONFIRM_PASSWORD, confirmPassword);
        validation.keep();
        changePassword();
    }


}

package securesocial.provider;

/**
 * This is the interface that defines the behaviour for UserService.
 * There is a default implementation in the DefaultUserService class that
 * stores things in a map.  This is just to provide an example, as a real
 * implementation should persist things in a database
 *
 * @see DefaultUserService
 */
public interface UserServiceDelegate {
    /**
     * Finds a SocialUser that matches the id
     *
     * @param id A UserId object
     * @return A SocialUser instance or null if no user matches the specified id.
     */
    SocialUser find(UserId id);

    /**
     * Finds a SocialUser that matches the given email
     *
     * @param email email address to search with
     * @return A SocialUser instance with the specified email, or null if none is found
     */
    SocialUser find(String email);

    /**
     * Saves the user in the backing store.
     *
     * @param user A SocialUser object
     */
    void save(SocialUser user);

    /**
     * Creates an activation request.  This is needed for users that
     * are creating an account in the system instead of using one in a 3rd party system.
     *
     * @param user The user that needs to be activated
     * @return A string with a uuid that will be embedded in the welcome email.
     */
    String createActivation(SocialUser user);

    /**
     * Activates a user by setting the isEmailVerified field to true.  This is only used
     * for UsernamePassword accounts.
     *
     * @param uuid The uuid created using the createActivation method.
     * @return Returns true if the user was activated - false otherwise.
     */
    boolean activate(String uuid);

    /**
     * Creates an password reset request.  This is needed for users with username/password
     * authentication that have lost their password, and need to set a new one
     *
     * @param user The user that needs to be reset the password
     * @return A string with a uuid that will be embedded in the password reset email.
     */
    String createPasswordReset(SocialUser user);

    /**
     * Return the SocialUser for this reset request, if such a request exists.
     *
     * @param username the username for this reset request
     * @param uuid     random uuid, i.e. the one-time-password for this password reset
     */
    SocialUser fetchForPasswordReset(String username, String uuid);

    /**
     * Disable the reset code for this user, once the password has been changed
     *
     * @param username the username for this reset request
     * @param uuid     random uuid, i.e. the one-time-password for this password reset
     */
    void disableResetCode(String username, String uuid);


    /**
     * This method deletes activations that were not completed by the user (The user did not follow the link
     * in the welcome email).
     * <p/>
     * The method should delete the information store for the user too.
     * store for the user.
     */
    void deletePendingActivations();
}
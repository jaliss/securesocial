package securesocial.core.java;

/**
 * The password information
 */
public class PasswordInfo {
    /**
     * The hashed user password
     */
    public String password;

    /**
     * The salt used to hash the password
     */
    public String salt;

    public static PasswordInfo fromScala(securesocial.core.PasswordInfo scalaInfo) {
        PasswordInfo result = new PasswordInfo();
        result.password = scalaInfo.password();
        if ( scalaInfo.salt().isDefined() ) {
            result.salt = scalaInfo.salt().get();
        }
        return result;
    }
}

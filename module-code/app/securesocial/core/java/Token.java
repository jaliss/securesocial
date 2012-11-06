package securesocial.core.java;

import org.joda.time.DateTime;

/**
 * A token used for reset password and sign up operations
 */
public class Token {
    public String uuid;
    public String email;
    public DateTime creationTime;
    public DateTime expirationTime;
    public boolean isSignUp;

    public boolean isExpired() {
        return expirationTime.isBeforeNow();
    }

    public securesocial.core.providers.Token toScala() {
        return securesocial.core.providers.Token$.MODULE$.apply(
                uuid, email, creationTime, expirationTime, isSignUp
        );
    }

    public static Token fromScala(securesocial.core.providers.Token scalaToken) {
        Token javaToken = new Token();
        javaToken.uuid = scalaToken.uuid();
        javaToken.email = scalaToken.email();
        javaToken.creationTime = scalaToken.creationTime();
        javaToken.expirationTime = scalaToken.expirationTime();
        javaToken.isSignUp = scalaToken.isSignUp();
        return javaToken;
    }
}

package securesocial.utils;

/**
 * Wrapper interface to enable configuring the password hashing method per project basis
 */
public interface PasswordHasher {

    String passwordHash(String password);

    boolean verifyPasswordHash(String provided, String storedHash);

}

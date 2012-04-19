package securesocial.utils;

import play.Play;
import securesocial.plugin.SecureSocialPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SecureSocialPasswordHasher {

    private static PasswordHasher hasher;

    public static void setPasswordHasher(PasswordHasher concrete) {
        hasher = concrete;
    }


    public static String passwordHash(String password) {
        return hasher.passwordHash(password);
    }

    public static boolean verifyPasswordHash(String candidate, String storedHash) {
        return hasher.verifyPasswordHash(candidate, storedHash);
    }


}

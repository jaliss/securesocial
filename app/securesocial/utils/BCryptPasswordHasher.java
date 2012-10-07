package securesocial.utils;

import play.Play;
import securesocial.plugin.SecureSocialPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class BCryptPasswordHasher {

    public static String passwordHash(String password) {
	return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verifyPasswordHash(String candidate, String storedHash) {
	return BCrypt.checkpw(candidate, storedHash);
    }


}

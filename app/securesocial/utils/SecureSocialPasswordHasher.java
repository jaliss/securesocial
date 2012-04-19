package securesocial.utils;

import play.Play;
import securesocial.plugin.SecureSocialPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SecureSocialPasswordHasher {

    public static final String SECURE_SOCIAL_PASSWORD_HASHER = SecureSocialPlugin.SECURESOCIAL + ".password.hasher";

    private static final PasswordHasher hasher;

    static {
        if (Play.configuration.contains(SECURE_SOCIAL_PASSWORD_HASHER)) {
            //Use the default one, i.e. PlayCryptoHasher
            hasher = new PlayCryptoHasher();
        } else {
            // Try to initialize the provider hasher type
            String className = Play.configuration.getProperty(SECURE_SOCIAL_PASSWORD_HASHER);
            try {
                Class<?> clazz = Class.forName(className);
                Class<? extends PasswordHasher> pwdh = clazz.asSubclass(PasswordHasher.class);

                if (!pwdh.isAssignableFrom(PasswordHasher.class)) {
                    throw new RuntimeException("The provided hasher implementation class (" + className + ") is not a subclass of password hasher");
                }

                Constructor<? extends PasswordHasher> constructor = pwdh.getConstructor();

                // Assign the hasher to be used for the rest of the lifecycle
                hasher = constructor.newInstance();

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Cannot initialize the requested hasher class " + className, e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Cannot initialize the requested hasher class " + className + " as no suitable, no args constructor found", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Cannot initialize the requested hasher class " + className + ". Error when invoking the constructor", e);
            } catch (InstantiationException e) {
                throw new RuntimeException("Cannot initialize the requested hasher class " + className + " . Error when instantiating", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot initialize the requested hasher class " + className + ". A public no args constructor cannot be found", e);
            }
        }
    }


    public static String passwordHash(String password) {
        return hasher.passwordHash(password);
    }

    public static boolean verifyPasswordHash(String candidate, String storedHash) {
        return hasher.verifyPasswordHash(candidate, storedHash);
    }


}

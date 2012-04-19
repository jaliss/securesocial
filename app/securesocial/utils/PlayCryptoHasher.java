package securesocial.utils;

import play.libs.Crypto;

/**
 * Default hasher for SecureSocial, which works the same way as the
 * direct call to <code>play.libs.Crypto</code>, but adds a wrapper
 * around the calls, to enable configuring the implementing class
 */
public class PlayCryptoHasher implements PasswordHasher {

    @Override
    public String passwordHash(String password) {
        return Crypto.passwordHash(password);
    }

    @Override
    public boolean verifyPasswordHash(String provided, String storedHash) {
        if (provided != null && storedHash != null) {
            String hash = passwordHash(provided);

            return storedHash.equals(hash);
        }

        return false;
    }


}

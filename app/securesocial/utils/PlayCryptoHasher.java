package securesocial.utils;

import play.libs.Crypto;

/**
 * Default hasher for SecureSocial, which works the same way as the
 * direct call to <code>play.libs.Crypto</code>, but adds a wrapper
 * around the calls, to enable configuring the implementing class
 */
public class PlayCryptoHasher implements PasswordHasher {

    private final Crypto.HashType hashType;

    public PlayCryptoHasher() {
        this(Crypto.HashType.MD5);
    }

    public PlayCryptoHasher(Crypto.HashType hashType) {
        this.hashType = hashType;
    }

    @Override
    public String passwordHash(String password) {
        return Crypto.passwordHash(password, hashType);
    }

    @Override
    public boolean verifyPasswordHash(String candidate, String storedHash) {
        if (candidate != null && storedHash != null) {
            String hash = passwordHash(candidate);

            return storedHash.equals(hash);
        }

        return false;
    }


}

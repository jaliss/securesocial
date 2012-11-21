/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.core.java;

import play.Application;
import play.Plugin;
import securesocial.core.providers.utils.PasswordHasher;

/**
 * Base class for all password hashers written in Java.
 *
 * Note: You need to implement all the doXXX methods below.
 */
public abstract class BasePasswordHasher implements PasswordHasher {
    protected Application application;

    public BasePasswordHasher(Application application) {
        this.application = application;
    }

    @Override
    public securesocial.core.PasswordInfo hash(String plainPassword) {
         return doHash(plainPassword).toScala();
    }

    @Override
    public boolean matches(securesocial.core.PasswordInfo passwordInfo, String suppliedPassword) {
        return doMatch(PasswordInfo.fromScala(passwordInfo), suppliedPassword);
    }

    /**
     * Hashes a password
     *
     * @param plainPassword the password to hash
     * @return a PasswordInfo containting the hashed password and optional salt
     */
    abstract PasswordInfo doHash(String plainPassword);

    /**
     * Checks whether a supplied password matches the hashed one
     *
     * @param passwordInfo the password retrieved from the backing store (by means of UserService)
     * @param suppliedPassword the password supplied by the user trying to log in
     * @return true if the password matches, false otherwise.
     */
    abstract boolean doMatch(PasswordInfo passwordInfo, String suppliedPassword);
}

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

import play.libs.Scala;

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

    /**
     * The hasher used to create this password info
     */
    public String hasher;

    public securesocial.core.PasswordInfo toScala() {
        return securesocial.core.PasswordInfo$.MODULE$.apply(hasher, password, Scala.Option(salt));
    }

    public static PasswordInfo fromScala(securesocial.core.PasswordInfo scalaInfo) {
        PasswordInfo result = new PasswordInfo();
        result.password = scalaInfo.password();
        result.hasher = scalaInfo.hasher();
        if ( scalaInfo.salt().isDefined() ) {
            result.salt = scalaInfo.salt().get();
        }
        return result;
    }
}

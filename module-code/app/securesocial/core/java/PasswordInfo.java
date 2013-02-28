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
import scala.Option;
import scala.Some;

/**
 * The password information
 */
public class PasswordInfo {

    /**
     * The hasher that was used to hash the password.
     */
    public String hasher;
    /**
     * The hashed user password
     */
    public String password;

    /**
     * The salt used to hash the password
     */
    public String salt;

    public PasswordInfo() {
    }

    public PasswordInfo(final String hasher, final String password, final String salt) {
        this.hasher = hasher;
        this.password = password;
        this.salt = salt;
    }

    public static Option<securesocial.core.PasswordInfo> toScala(final PasswordInfo javaInfo) {
        if (javaInfo == null) {
            return Option.empty();
        }
        return new Some<>(new securesocial.core.PasswordInfo(javaInfo.hasher, javaInfo.password,
                Option.apply(javaInfo.salt)));
    }

    public static PasswordInfo fromScala(final Option<securesocial.core.PasswordInfo> scalaInfoOption) {
        if (!scalaInfoOption.isDefined()) {
            return null;
        }
        return fromScala(scalaInfoOption.get());
    }

    public static PasswordInfo fromScala(final securesocial.core.PasswordInfo scalaInfo) {
        if (scalaInfo == null) {
            return null;
        }
        return new PasswordInfo(scalaInfo.hasher(), scalaInfo.password(), Scala.orNull(scalaInfo.salt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hasher == null) ? 0 : hasher.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((salt == null) ? 0 : salt.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PasswordInfo other = (PasswordInfo) obj;
        if (hasher == null) {
            if (other.hasher != null) {
                return false;
            }
        } else if (!hasher.equals(other.hasher)) {
            return false;
        }
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (salt == null) {
            if (other.salt != null) {
                return false;
            }
        } else if (!salt.equals(other.salt)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PasswordInfo [hasher=" + hasher + ", password=" + password + ", salt=" + salt + "]";
    }
}

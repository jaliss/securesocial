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
 * The OAuth2 information
 */
public class OAuth2Info {

    public String accessToken;
    public String tokenType;
    public Integer expiresIn;
    public String refreshToken;

    public OAuth2Info() {
    }

    public OAuth2Info(final String accessToken, final String tokenType, final Integer expiresIn,
            final String refreshToken) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
    }

    public static Option<securesocial.core.OAuth2Info> toScala(final OAuth2Info javaInfo) {
        if (javaInfo == null) {
            return Option.empty();
        }
        return new Some<>(new securesocial.core.OAuth2Info(javaInfo.accessToken, Option.apply(javaInfo.tokenType),
                Option.<Object> apply(javaInfo.expiresIn), Option.apply(javaInfo.refreshToken)));
    }

    /**
     * This translates the scala version of OAuth2Info to the Java version
     * @param scalaInfo
     * @return
     */
    public static OAuth2Info fromScala(final Option<securesocial.core.OAuth2Info> scalaInfoOption) {
        if (!scalaInfoOption.isDefined()) {
            return null;
        }
        final securesocial.core.OAuth2Info scalaInfo = scalaInfoOption.get();
        final OAuth2Info result = new OAuth2Info(scalaInfo.accessToken(), Scala.orNull(scalaInfo.tokenType()),
                (Integer) Scala.orNull(scalaInfo.expiresIn()), Scala.orNull(scalaInfo.refreshToken()));
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accessToken == null) ? 0 : accessToken.hashCode());
        result = prime * result + ((expiresIn == null) ? 0 : expiresIn.hashCode());
        result = prime * result + ((refreshToken == null) ? 0 : refreshToken.hashCode());
        result = prime * result + ((tokenType == null) ? 0 : tokenType.hashCode());
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
        final OAuth2Info other = (OAuth2Info) obj;
        if (accessToken == null) {
            if (other.accessToken != null) {
                return false;
            }
        } else if (!accessToken.equals(other.accessToken)) {
            return false;
        }
        if (expiresIn == null) {
            if (other.expiresIn != null) {
                return false;
            }
        } else if (!expiresIn.equals(other.expiresIn)) {
            return false;
        }
        if (refreshToken == null) {
            if (other.refreshToken != null) {
                return false;
            }
        } else if (!refreshToken.equals(other.refreshToken)) {
            return false;
        }
        if (tokenType == null) {
            if (other.tokenType != null) {
                return false;
            }
        } else if (!tokenType.equals(other.tokenType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OAuth2Info [accessToken=" + accessToken + ", tokenType=" + tokenType + ", expiresIn=" + expiresIn
                + ", refreshToken=" + refreshToken + "]";
    }
}

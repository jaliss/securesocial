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

/**
 * Authentication methods used by the identity providers
 */
public enum AuthenticationMethod {
    OAUTH1,
    OAUTH2,
    OPENID,
    USERNAME_PASSWORD;

    public static AuthenticationMethod fromScala(securesocial.core.AuthenticationMethod scalaMethod) {
        final AuthenticationMethod result;

        if ( scalaMethod.equals( securesocial.core.AuthenticationMethod.OAuth1()) ) {
            result = OAUTH1;
        } else if ( scalaMethod.equals(securesocial.core.AuthenticationMethod.OAuth2())) {
            result = OAUTH2;
        } else if ( scalaMethod.equals(securesocial.core.AuthenticationMethod.OpenId())) {
            result = OPENID;
        } else if ( scalaMethod.equals(securesocial.core.AuthenticationMethod.UserPassword())) {
            result = USERNAME_PASSWORD;
        } else {
            throw new RuntimeException("Unknown authentication method: " + scalaMethod.toString());
        }
        return result;
    }

    public static securesocial.core.AuthenticationMethod toSala(AuthenticationMethod method) {
        securesocial.core.AuthenticationMethod result = null;

        switch (method) {
            case OAUTH1:
                result = securesocial.core.AuthenticationMethod.OAuth1();
                break;
            case OAUTH2:
                result = securesocial.core.AuthenticationMethod.OAuth2();
                break;
            case OPENID:
                result = securesocial.core.AuthenticationMethod.OpenId();
                break;
            case USERNAME_PASSWORD:
                result = securesocial.core.AuthenticationMethod.UserPassword();
                break;
        }
        return result;
    }
}

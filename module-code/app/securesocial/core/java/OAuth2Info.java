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
 * The OAuth2 information
 */
public class OAuth2Info {
    public String accessToken;
    public String tokenType;
    public Integer expiresIn;
    public String refreshToken;

    /**
     * This translates the scala version of OAuth2Info to the Java version
     * @param scalaInfo
     * @return
     */
    public static OAuth2Info fromScala(securesocial.core.OAuth2Info scalaInfo) {
        OAuth2Info result = new OAuth2Info();
        result.accessToken = scalaInfo.accessToken();

        if ( scalaInfo.tokenType().isDefined() ) {
            result.tokenType = scalaInfo.tokenType().get();
        }

        if ( scalaInfo.expiresIn().isDefined() ) {
            result.expiresIn = (Integer) scalaInfo.expiresIn().get();
        }

        if ( scalaInfo.refreshToken().isDefined() ) {
            result.refreshToken = scalaInfo.refreshToken().get();
        }

        return result;
    }
}

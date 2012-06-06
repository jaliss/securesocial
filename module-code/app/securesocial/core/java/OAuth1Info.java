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

import play.api.libs.oauth.ServiceInfo;

/**
 * The OAuth1 information
 */
public class OAuth1Info {
    /**
     * The ServiceInfo holds the information to identify an oauth provider.
     *
     * Note: this value does not need to be persisted by UserService since it is set automatically
     * in the SecureSocial Controller for each request that needs it.
     */
    public ServiceInfo serviceInfo;

    /**
     * The token returned by the OAuth provider
     */
    public String token;

    /**
     * The secret returned by the OAuth provider
     */
    public String secret;

    /**
     * This translates the scala version of OAuth1Info to the Java version
     *
     * @param info
     * @return
     */
    public static OAuth1Info fromScala(securesocial.core.OAuth1Info info) {
        OAuth1Info result = new OAuth1Info();
        result.token = info.token();
        result.secret = info.secret();
        // I'm not providing a serviceInfo wrapper for now (I think this is going to be added to Play)
        result.serviceInfo = info.serviceInfo();
        return result;
    }
}

/**
* Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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

import play.mvc.Http;
import securesocial.core.RuntimeEnvironment;

/**
*
*/
public class SecureSocial {
    /**
     * The user key
     */
    public static final String USER_KEY = "securesocial.user";

    /**
     * The original url key
     */
    public static final String ORIGINAL_URL = "original-url";

    /**
     * Returns the current environment.
     */
    public static RuntimeEnvironment env() {
        return (RuntimeEnvironment) Http.Context.current().args.get("securesocial-env");
    }
}

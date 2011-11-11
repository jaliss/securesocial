/**
* Copyright 2011 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.provider;

/**
 * A class to uniquely identify users. This combines the id the user has on
 * an external service (eg: twitter, facebook) with the provider type.
 */
public class UserId implements java.io.Serializable {
    /**
     * The id the user has in a external service.
     */
    public String id;

    /**
     * The provider this user belongs to.
     */
    public ProviderType provider;
}

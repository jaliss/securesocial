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

import play.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Provider Registry.  All the providers discovered by the bootstrap job are registered here.
 * The SecureSocial controller looks for the providers here as well.
 *
 * @see securesocial.plugin.SecureSocialPlugin
 * @see controllers.securesocial.SecureSocial
 */
public class ProviderRegistry {
    private static Map<ProviderType, IdentityProvider> providers = new LinkedHashMap<ProviderType, IdentityProvider>();

    /**
     * Registgers a provider.
     *
     * @param p The identity provider.
     * @throws RuntimeException if there is another provider registered with the same type.
     * @see ProviderType
     */
    public static void register(IdentityProvider p) {
        if ( providers.get(p.type) != null ) {
            // make sure the same type is not used more than once
            Logger.error("Tried to register provider for type: " + p.type + " but it is registered already.  Providers: " + providers);
            throw new RuntimeException("There is already a provider registered for type: " + p.type);
        }
        providers.put(p.type, p);
        Logger.info("Registered Identity Provider: " + p.type);
    }

    /**
     * Returns a provider that matches the specified type.
     *
     * @param type A ProviderType
     * @return An IdentityProvider
     */
    public static IdentityProvider get(ProviderType type) {
        return providers.get(type);
    }

    /**
     * Returns all the registered providers.
     * @return A Collection with the providers
     */
    public static Collection<IdentityProvider> all() {
        return providers.values();
    }
}

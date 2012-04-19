/**
* Copyright 2011 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss and
*                Francis De Brabandere - twitter: @somatik 
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
package securesocial.plugin;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import securesocial.provider.DefaultUserService;
import securesocial.provider.IdentityProvider;
import securesocial.provider.ProviderRegistry;
import securesocial.provider.ProviderType;
import securesocial.provider.UserService;

public class SecureSocialPlugin extends PlayPlugin {
    public static final String SECURESOCIAL = "securesocial";
    private static final String SECURESOCIAL_PROVIDERS = SECURESOCIAL + ".providers";
    private static final String SEPARATOR = ",";

    @Override
    public void onApplicationStart() {
    	// register providers
        final List<Class> providers = Play.classloader.getAssignableClasses(IdentityProvider.class);        
        if ( providers.size() > 0 ) {
            Map<ProviderType, IdentityProvider> availableProviders = new LinkedHashMap<ProviderType, IdentityProvider>();
            for( Class clazz : providers ) {
                if ( !Modifier.isAbstract(clazz.getModifiers()) ) {
                    IdentityProvider provider = (IdentityProvider) newInstance(clazz);
                        availableProviders.put(provider.type, provider);
                }
            }
            // register them in the preferred order
            final String s = Play.configuration.getProperty(SECURESOCIAL_PROVIDERS);

            if ( s != null && s.length() > 0) {
                final String[] requestedProviders = s.split(SEPARATOR);
                for ( String type: requestedProviders ) {
                    try {
                        ProviderRegistry.register(availableProviders.get(ProviderType.valueOf(type)));
                    } catch ( IllegalArgumentException e ) {
                        Logger.error("Unknown type specified in securesocial.providers: %s", type);
                    }
                }
            } else {
                for( IdentityProvider p : availableProviders.values()) {
                    ProviderRegistry.register(p);
                }
            }
        } else {
            Logger.fatal("Unable to find identity providers.");
        }

        // set the user service
        final List<Class> classes = Play.classloader.getAssignableClasses(UserService.Service.class);
        UserService.Service service = null;

        int classesFound = classes.size();
        if ( classesFound == 1 ) {
            // use the default implementation
            Logger.info("Using default user service");
            service = new DefaultUserService();
        } else if ( classesFound == 2 ) {
            // a custom implementation was found.  use it instead of the default
            Class clazz = classes.get(0);
            if ( clazz.getName().startsWith(SECURESOCIAL) ) {
                clazz = classes.get(1);
            }
            service = (UserService.Service) newInstance(clazz);
            Logger.info("Using custom user service: %s", service.getClass());
        } else {
            // should not happen unless someone implements the interface more than once.
            Logger.fatal("More than one custom UserService was found.  Unable to initialize.");
        }
        UserService.setService(service);
    }
    
    private Object newInstance(Class<?> cls){
        try {
            return cls.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
        	throw new RuntimeException(e);
        }
    }
}

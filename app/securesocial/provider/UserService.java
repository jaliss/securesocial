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
 * A class that provides the means to find save and create users
 * for the SecureSocial Module.
 *
 * @see DefaultUserService
 */
public class UserService {
    private static ApplicationUserService service;

    /**
     * Sets the Service implementation that will be used.
     *
     * @param delegate A Service instance.
     * @see securesocial.plugin.SecureSocialPlugin
     */
    public static void setService(ApplicationUserService delegate) {
        service = delegate;
    }

    /**
     * @see securesocial.provider.ApplicationUserService#find(UserId)
     */
    public static SocialUser find(UserId id) {
        checkIsInitialized();
        return service.find(id);
    }

    /**
     * @see securesocial.provider.ApplicationUserService#find(String)
     */
    public static SocialUser find(String email) {
        checkIsInitialized();
        return service.find(email);
    }

    private static void checkIsInitialized() {
        if (service == null) {
            throw new RuntimeException("UserService was not properly initialized.");
        }
    }

    /**
     * @see securesocial.provider.ApplicationUserService#save(SocialUser)
     */
    public static void save(SocialUser user) {
        checkIsInitialized();
        service.save(user);
    }

    /**
     * @see securesocial.provider.ApplicationUserService#createActivation(SocialUser)
     */
    public static String createActivation(SocialUser user) {
        checkIsInitialized();
        return service.createActivation(user);
    }

    /**
     * @see securesocial.provider.ApplicationUserService#activate(String)
     */
    public static boolean activate(String uuid) {
        checkIsInitialized();
        return service.activate(uuid);
    }

    /**
     * @see securesocial.provider.ApplicationUserService#createActivation(SocialUser)
     */
    public static String createPasswordReset(SocialUser user) {
        checkIsInitialized();
        return service.createPasswordReset(user);
    }

    /**
     * @see securesocial.provider.ApplicationUserService#fetchForPasswordReset(String, String)
     */
    public static SocialUser fetchForPasswordReset(String user, String uuid) {
        checkIsInitialized();
        return service.fetchForPasswordReset(user, uuid);
    }


    /**
     * @see securesocial.provider.ApplicationUserService#disableResetCode(String, String)
     */
    public static void disableResetCode(String username, String uuid) {
        checkIsInitialized();
        service.disableResetCode(username, uuid);
    }

    /**
     * @see securesocial.provider.ApplicationUserService#deletePendingActivations()
     */
    public static void deletePendingActivations() {
        checkIsInitialized();
        service.deletePendingActivations();
    }
}

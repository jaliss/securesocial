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

import play.libs.Codec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The default user service provided with SecureSocial.
 * If users need to find/save users in a custom backing store they only
 * need to provide an implementation of the UserService.Service interface in their app. It will be picked up automatically.
 * <p/>
 * This class it not suitable for a production environment.  It is only meant to be used in development.  For production use
 * you need to provide your own implementation.
 *
 * @see UserServiceDelegate
 * @see securesocial.plugin.SecureSocialPlugin
 */
public class DefaultUserService implements UserServiceDelegate {

    private Map<String, SocialUser> users = Collections.synchronizedMap(new HashMap<String, SocialUser>());
    private Map<String, SocialUser> activations = Collections.synchronizedMap(new HashMap<String, SocialUser>());
    private Map<String, SocialUser> resetRequests = Collections.synchronizedMap(new HashMap<String, SocialUser>());


    public SocialUser find(UserId id) {
        return users.get(id.id + id.provider.toString());
    }

    public SocialUser find(String email) {
        for (SocialUser su : users.values()) {
            if (su.email.equals(email)) {
                return su;
            }
        }

        return null;
    }

    public void save(SocialUser user) {
        users.put(user.id.id + user.id.provider.toString(), user);
    }

    public String createActivation(SocialUser user) {
        final String uuid = Codec.UUID();
        activations.put(uuid, user);
        return uuid;
    }

    public boolean activate(String uuid) {
        SocialUser user = activations.get(uuid);
        boolean result = false;

        if (user != null) {
            user.isEmailVerified = true;
            save(user);
            activations.remove(uuid);
            result = true;
        }
        return result;
    }

    @Override
    public String createPasswordReset(SocialUser user) {
        final String uuid = Codec.UUID();
        resetRequests.put(uuid, user);
        return uuid;
    }

    @Override
    public SocialUser fetchForPasswordReset(String username, String uuid) {
        if (!resetRequests.containsKey(uuid)) {
            return null;
        }

        SocialUser socialUser = resetRequests.get(uuid);
        if (socialUser.id.id.equals(username)) {
            return socialUser;
        }

        return null;
    }

    @Override
    public void disableResetCode(String username, String uuid) {
        SocialUser socialUser = fetchForPasswordReset(username, uuid);
        if (socialUser != null) {
            resetRequests.remove(uuid);
        }
    }

    public void deletePendingActivations() {
        activations.clear();
    }
}

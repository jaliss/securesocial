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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The default user service provided with SecureSocial.
 * If users need to find/save users in a custom backing store they only
 * need to provide an implementation of the UserService.Service interface in their app. It will be picked up automatically.
 *
 * This class it not suitable for a production environment.  It is only meant to be used in development.  For production use
 * you need to provide your own implementation.
 *
 * @see UserService.Service
 * @see securesocial.jobs.Bootstrap
 */
public class DefaultUserService implements UserService.Service {

    private Map<String, SocialUser> users = Collections.synchronizedMap(new HashMap<String, SocialUser>());
    private Map<String, SocialUser> activations = Collections.synchronizedMap(new HashMap<String, SocialUser>());

    public SocialUser find(UserId id) {
        return users.get(id.id + id.provider.toString());
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

		// note: at this point the user already exists in the, it's just an
		// update
        if( user != null ) {
            user.isEmailVerified =  true;
            save(user);
            activations.remove(uuid);
            result = true;
        }
        return result;
    }

    public void deletePendingActivations() {
       	// currently this job is ran every 24 hours so I am matching the logic
		Date currentDate = new Date();
		Iterator<Entry<String, SocialUser>> it = activations.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, SocialUser> entry = it.next();
			SocialUser s = entry.getValue();
			
			// get the time and add 24H
			Date storedDate = s.lastAccess;
			Calendar c = Calendar.getInstance();
			c.setTime(storedDate);
			c.add(Calendar.HOUR, 24);

			// if the time has expired then clean
			if (currentDate.after(c.getTime())) {
				// we should delete from users or else junk will build up and
				// take up usernames when people are trying to register
				delete(s.id);
				// think about error handling here

				// remove from the activation records
				it.remove(); 
			}
		}

	}

	// should this be added to the interface?
	private boolean delete(UserId userId) {
		SocialUser user = users.remove(userId.id + userId.provider.toString());
		return user == null ? false : true;
	}
}

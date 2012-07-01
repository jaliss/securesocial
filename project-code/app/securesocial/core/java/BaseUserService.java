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

import play.Application;
import play.libs.Scala;
import scala.Option;

import java.lang.reflect.Field;

/**
 * A base user service for developers that want to write their UserService in Java.
 * The save() and find() methods handle the Scala<->Java conversions.
 *
 * Subclasses need to implement the doSave and doFind mehtods.
 *
 */
public abstract class BaseUserService extends securesocial.core.UserServicePlugin {

    public static final String APPLICATION = "application";

    // a bit of black magic to be able to extend a Scala plugin :)
    private static play.api.Application toScala(Application app) {
        try {
            Field field = app.getClass().getDeclaredField(APPLICATION);
            field.setAccessible(true);
            return (play.api.Application) field.get(app);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize user service", e);
        }
    }

    public BaseUserService(Application application) {
        super(toScala(application));
    }

    /**
     * Finds a SocialUser that maches the specified id
     *
     * @param id the user id
     * @return an optional user
     */
    @Override
    public Option<securesocial.core.SocialUser> find(securesocial.core.UserId id) {
        UserId javaId = new UserId();
        javaId.id = id.id();
        javaId.provider = id.providerId();
        SocialUser javaUser = doFind(javaId);
        securesocial.core.SocialUser scalaUser = null;
        if ( javaUser != null ) {
            scalaUser = javaUser.toScala();
        }
        return Scala.Option(scalaUser);
    }

    /**
     * Saves the user.  This method gets called when a user logs in.
     * This is your chance to save the user information in your backing store.
     * @param user
     */
    @Override
    public void save(securesocial.core.SocialUser user) {
        doSave(SocialUser.fromScala(user));
    }

    /**
     * Saves the user in the backing store
     *
     * @param user
     */
    public abstract void doSave(SocialUser user);

    /**
     * Finds the user in the backing store.
     */
    public abstract SocialUser doFind(UserId userId);

}

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
 *
 * Note: You need to implement all the doXXX methods below.
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
     * Finds a Social user by email and provider id.
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation.
     *
     * @param email - the user email
     * @param providerId - the provider id
     * @return
     */
    @Override
    public Option<securesocial.core.SocialUser> findByEmailAndProvider(String email, String providerId) {
        SocialUser javaUser = doFindByEmailAndProvider(email, providerId);
        securesocial.core.SocialUser scalaUser = javaUser != null ? javaUser.toScala() : null;
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
     * Saves a token.  This is needed for users that
     * are creating an account in the system instead of using one in a 3rd party system.
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param token The token to save
     * @return A string with a uuid that will be embedded in the welcome email.
     */
    @Override
    public void save(securesocial.core.providers.Token token) {
        doSave(Token.fromScala(token));
    }

    /**
     * Finds a token
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param token the token id
     * @return
     */
    @Override
    public Option<securesocial.core.providers.Token> findToken(String token) {
        Token javaToken = doFindToken(token);
        securesocial.core.providers.Token scalaToken = javaToken != null ? javaToken.toScala() : null;
        return Scala.Option(scalaToken);
    }

    /**
     * Deletes a token
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param uuid the token id
     */
    @Override
    public void deleteToken(String uuid) {
        doDeleteToken(uuid);
    }

    /**
     * Deletes all expired tokens
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     */
    @Override
    public void deleteExpiredTokens() {
        doDeleteExpiredTokens();
    }

    /**
     * Saves the user.  This method gets called when a user logs in.
     * This is your chance to save the user information in your backing store.
     * @param user
     */
    public abstract void doSave(SocialUser user);

    /**
     * Saves a token
     *
     * @param token
     */
    public abstract void doSave(Token token);

    /**
     * Finds the user in the backing store.
     */
    public abstract SocialUser doFind(UserId userId);

    /**
     * Finds a token
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param tokenId the token id
     * @return
     */
    public abstract Token doFindToken(String tokenId);


    /**
     * Finds a Social user by email and provider id.
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation.
     *
     * @param email - the user email
     * @param providerId - the provider id
     * @return
     */
    public abstract SocialUser doFindByEmailAndProvider(String email, String providerId);

    /**
     * Deletes a token
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param uuid the token id
     */
    public abstract void doDeleteToken(String uuid);

    /**
     * Deletes all expired tokens
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     */
    public abstract void doDeleteExpiredTokens();
}

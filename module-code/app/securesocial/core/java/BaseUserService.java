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

import play.libs.F;
import play.libs.Scala;
import scala.*;
import scala.Option;
import scala.concurrent.Future;
import securesocial.core.BasicProfile;
import securesocial.core.PasswordInfo;
import securesocial.core.providers.MailToken;
import securesocial.core.services.SaveMode;
import securesocial.core.services.UserService;

import java.lang.Boolean;

/**
 * A base user service for developers that want to write their UserService in Java.
 *
 * Note: You need to implement all the doXXX methods below.
 *
 */
public abstract class BaseUserService<U> implements UserService<U> {
    protected BaseUserService() {
    }

    /**
     * Finds an Identity that maches the specified id
     *
     * @return an optional user
     */
    @Override
    public Future<Option<BasicProfile>> find(String providerId, String userId) {
        return doFind(providerId, userId).map(new F.Function<BasicProfile, Option<BasicProfile>>() {
            @Override
            public Option<BasicProfile> apply(BasicProfile user) throws Throwable {
                return Scala.Option(user);
            }
        }).wrapped();
    }

    /**
     * Finds an Identity by email and provider id.
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation.
     *
     * @param email - the user email
     * @param providerId - the provider id
     * @return
     */
    @Override
    public Future<Option<BasicProfile>> findByEmailAndProvider(String email, String providerId) {
        return doFindByEmailAndProvider(email, providerId).map(new F.Function<BasicProfile, Option<BasicProfile>>() {
            public Option<BasicProfile> apply(BasicProfile user) throws Throwable {
                return Scala.Option(user);
            }
        }).wrapped();
    }

    /**
     * Saves the Identity.  This method gets called when a user logs in.
     * This is your chance to save the user information in your backing store.
     *
     * @param user
     */
    @Override
    public Future<U> save(BasicProfile user, SaveMode mode) {
        return doSave(user, mode).wrapped();
    }

    /**
     * Links the current user Identity to another
     *
     * @param current The Identity of the current user
     * @param to The Identity that needs to be linked to the current user
     */
    @Override
    public Future<U> link(U current, BasicProfile to) {
        return doLink(current, to).wrapped();
    }

    @Override
    public Future<scala.Option<PasswordInfo>> passwordInfoFor(U user) {
        return doPasswordInfoFor(user).map(new F.Function<PasswordInfo, Option<PasswordInfo>>() {
            @Override
            public Option<PasswordInfo> apply(PasswordInfo passwordInfo) throws Throwable {
                return Scala.Option(passwordInfo);
            }
        }).wrapped();
    }

    @Override
    public Future<scala.Option<BasicProfile>> updatePasswordInfo(U user, PasswordInfo info) {
        return doUpdatePasswordInfo(user, info).map(new F.Function<BasicProfile, Option<BasicProfile>>() {
            @Override
            public Option<BasicProfile> apply(BasicProfile basicProfile) throws Throwable {
                return Scala.Option(basicProfile);
            }
        }).wrapped();
    }

    /**
     * Saves a token.  This is needed for users that
     * are creating an account in the system instead of using one in a 3rd party system.
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param mailToken The token to save
     * @return A string with a uuid that will be embedded in the welcome email.
     */
    @Override
    public Future<MailToken> saveToken(MailToken mailToken) {
        return doSaveToken(Token.fromScala(mailToken)).map(new F.Function<Token, MailToken>() {
            @Override
            public MailToken apply(Token token) throws Throwable {
                return token.toScala();
            }
        }).wrapped();
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
    public Future<Option<MailToken>> findToken(String token) {
        return doFindToken(token).map(new F.Function<Token, Option<MailToken>>() {
            @Override
            public Option<MailToken> apply(Token token) throws Throwable {
                MailToken scalaToken =  token  != null ? token.toScala() : null;
                return Scala.Option(scalaToken);
            }
        }).wrapped();
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
    public Future<scala.Option<MailToken>> deleteToken(String uuid) {
        return doDeleteToken(uuid).map(new F.Function<Token, Option<MailToken>>() {
            @Override
            public Option<MailToken> apply(Token token) throws Throwable {
                MailToken scalaToken =  token  != null ? token.toScala() : null;
                return Scala.Option(scalaToken);
            }
        }).wrapped();
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
     * Saves the Identity.  This method gets called when a user logs in.
     * This is your chance to save the user information in your backing store.
     *
     * @param user
     */
    public abstract F.Promise<U> doSave(BasicProfile user, SaveMode mode);

    /**
     * Saves a token
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param token
     */
    public abstract F.Promise<Token> doSaveToken(Token token);

    /**
     * Links the current user Identity to another
     *
     * @param current The Identity of the current user
     * @param to The Identity that needs to be linked to the current user
     */
    public abstract F.Promise<U> doLink(U current, BasicProfile to);

    /**
     * Finds the user in the backing store.
     * @return an Identity instance or null if no user matches the specified id
     */
    public abstract F.Promise<BasicProfile> doFind(String providerId, String userId);

    public abstract F.Promise<PasswordInfo>  doPasswordInfoFor(U user);

    public abstract F.Promise<BasicProfile> doUpdatePasswordInfo(U user, PasswordInfo info);

    /**
     * Finds a token
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param tokenId the token id
     * @return a Token instance or null if no token matches the specified id
     */
    public abstract F.Promise<Token> doFindToken(String tokenId);


    /**
     * Finds an identity by email and provider id.
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation.
     *
     * @param email - the user email
     * @param providerId - the provider id
     * @return an Identity instance or null if no user matches the specified id
     */
    public abstract F.Promise<BasicProfile> doFindByEmailAndProvider(String email, String providerId);

    /**
     * Deletes a token
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     * @param uuid the token id
     */
    public abstract F.Promise<Token> doDeleteToken(String uuid);

    /**
     * Deletes all expired tokens
     *
     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
     * implementation
     *
     */
    public abstract void doDeleteExpiredTokens();
}

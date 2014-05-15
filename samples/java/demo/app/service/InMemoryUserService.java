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
package service;

import play.Logger;
import play.libs.F;
import securesocial.core.BasicProfile;
import securesocial.core.PasswordInfo;
import securesocial.core.services.SaveMode;
import securesocial.core.java.BaseUserService;
import securesocial.core.java.Token;
import securesocial.core.providers.UsernamePasswordProvider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A Sample In Memory user service in Java
 *
 * Note: This is NOT suitable for a production environment and is provided only as a guide.
 * A real implementation would persist things in a database
 */
public class InMemoryUserService extends BaseUserService<DemoUser> {
    public Logger.ALogger logger = play.Logger.of("application.service.InMemoryUserService");

    private HashMap<String, DemoUser> users = new HashMap<String, DemoUser>();
    private HashMap<String, Token> tokens = new HashMap<String, Token>();

    @Override
    public F.Promise<DemoUser> doSave(BasicProfile profile, SaveMode mode) {
        DemoUser result = null;
        if (mode == SaveMode.SignUp()) {
            result = new DemoUser(profile);
            users.put(profile.providerId() + profile.userId(), result);
        } else if (mode == SaveMode.LoggedIn()) {
            for (Iterator<DemoUser> it =  users.values().iterator() ; it.hasNext() && result == null ; ) {
                DemoUser user = it.next();
                for ( BasicProfile p : user.identities) {
                    if ( p.userId().equals(profile.userId()) && p.providerId().equals(profile.providerId())) {
                        user.identities.remove(p);
                        user.identities.add(profile);
                        result = user;
                        break;
                    }
                }
            }
        } else if (mode == SaveMode.PasswordChange()) {
            for (Iterator<DemoUser> it =  users.values().iterator() ; it.hasNext() && result == null ; ) {
                DemoUser user = it.next();
                for (BasicProfile p : user.identities) {
                    if (p.userId().equals(profile.userId()) && p.providerId().equals(UsernamePasswordProvider.UsernamePassword())) {
                        user.identities.remove(p);
                        user.identities.add(profile);
                        result = user;
                        break;
                    }
                }
            }
        } else {
            throw new RuntimeException("Unknown mode");
        }
        return F.Promise.pure(result);
    }

    @Override
    public F.Promise<DemoUser> doLink(DemoUser current, BasicProfile to) {
        DemoUser target = null;

        for ( DemoUser u: users.values() ) {
            if ( u.main.providerId().equals(current.main.providerId()) && u.main.userId().equals(current.main.userId()) ) {
                target = u;
                break;
            }
        }

        if ( target == null ) {
            // this should not happen
            throw new RuntimeException("Can't find user : " + current.main.userId());
        }

        boolean alreadyLinked = false;
        for ( BasicProfile p : target.identities) {
            if ( p.userId().equals(to.userId()) && p.providerId().equals(to.providerId())) {
                alreadyLinked = true;
                break;
            }
        }
        if (!alreadyLinked) target.identities.add(to);
        return F.Promise.pure(target);
    }

    @Override
    public F.Promise<Token> doSaveToken(Token token) {
        tokens.put(token.uuid, token);
        return F.Promise.pure(token);
    }

    @Override
    public F.Promise<BasicProfile> doFind(String providerId, String userId) {
        if(logger.isDebugEnabled()){
            logger.debug("Finding user " + userId);
        }
        BasicProfile found = null;

        for ( DemoUser u: users.values() ) {
            for ( BasicProfile i : u.identities ) {
                if ( i.providerId().equals(providerId) && i.userId().equals(userId) ) {
                    found = i;
                    break;
                }
            }
        }

        return F.Promise.pure(found);
    }

    @Override
    public F.Promise<PasswordInfo> doPasswordInfoFor(DemoUser user) {
        throw new RuntimeException("doPasswordInfoFor is not implemented yet in sample app");
    }

    @Override
    public F.Promise<BasicProfile> doUpdatePasswordInfo(DemoUser user, PasswordInfo info) {
        throw new RuntimeException("doUpdatePasswordInfo is not implemented yet in sample app");
    }

    @Override
    public F.Promise<Token> doFindToken(String tokenId) {
        return F.Promise.pure(tokens.get(tokenId));
    }


    @Override
    public F.Promise<BasicProfile> doFindByEmailAndProvider(String email, String providerId) {
        BasicProfile found = null;

        for ( DemoUser u: users.values() ) {
            for ( BasicProfile i : u.identities ) {
                if ( i.providerId().equals(providerId) && i.email().isDefined() && i.email().get().equals(email) ) {
                    found = i;
                    break;
                }
            }
        }

        return F.Promise.pure(found);
    }

    @Override
    public F.Promise<Token> doDeleteToken(String uuid) {
        return F.Promise.pure(tokens.remove(uuid));
    }

    @Override
    public void doDeleteExpiredTokens() {
        Iterator<Map.Entry<String,Token>> iterator = tokens.entrySet().iterator();
        while ( iterator.hasNext() ) {
            Map.Entry<String, Token> entry = iterator.next();
            if ( entry.getValue().isExpired() ) {
                iterator.remove();
            }
        }
    }
}

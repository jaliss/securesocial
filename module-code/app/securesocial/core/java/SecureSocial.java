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

import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import scala.Option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the actions that can be used to protect controllers and retrieve the current user
 * if available.
 *
 * Sample usage:
 *
 *  @SecureSocial.Secured
 *  public static Result index() {
 *      SocialUser user = (SocialUser) ctx().args.get(SecureSocial.USER_KEY);
 *      return ok("Hello " + user.displayName);
 *  }
 */
public class SecureSocial {

    /**
     * The user key
     */
    public static final String USER_KEY = "securesocial.user";

    /**
     * The provider key
     */
    static final String PROVIDER_KEY = "securesocial.provider";

    /**
     * The original url key
     */
    static final String ORIGINAL_URL = "securesocial.originalUrl";

    /**
     * An annotation to mark actions as protected by SecureSocial
     * When the user is not logged in the action redirects the browser to the login page.
     *
     * If the isApiClient parameter is set to true SecureSocial will return a forbidden error
     * with a json error instead.
     */
    @With(SecuredAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Secured {
        /**
         * Specifies wether the action handles an API call or not. Default is false.
         * @return
         */
        boolean isApiClient() default false;
    }

    /**
     * Creates a UserId from the session
     *
     * @param ctx the current context
     * @return the UserId or null if there's no user in the session
     */
    private static securesocial.core.UserId getUserIdFromSession(Http.Context ctx) {
        final String user = ctx.session().get(USER_KEY);
        final String provider = ctx.session().get(PROVIDER_KEY);
        securesocial.core.UserId result = null;

        if ( user != null && provider != null ) {
            result = new securesocial.core.UserId(
                    user,
                    provider
            );
        }
        return result;
    }

    /**
     * Returns the current user
     *
     * @return a SocialUser or null if there is no current user
     */
     public static SocialUser currentUser() {
        SocialUser result = null;
        securesocial.core.UserId scalaUserId = getUserIdFromSession(Http.Context.current());

        if ( scalaUserId != null ) {
            Option<securesocial.core.SocialUser> option = securesocial.core.UserService$.MODULE$.find(scalaUserId);
            if ( option.isDefined() ) {
                securesocial.core.SocialUser scalaUser = securesocial.core.SecureSocial$.MODULE$.fillServiceInfo(option.get());
                result = SocialUser.fromScala(scalaUser);
            }
        }
        return result;
    }

    /**
     * Generates the json required for API calls.
     *
     * @return
     */
    private static ObjectNode forbiddenJson() {
        ObjectNode result = Json.newObject();
        result.put("error", "Credentials required");
        return result;
    }

    /**
     * Protects an action with SecureSocial
     */
    public static class SecuredAction extends Action<Secured> {

        @Override
        public Result call(Http.Context ctx) throws Throwable {
            try {
                // I don't understand why the ctx is not set in the Http.Context thread local variable.
                // I'm setting it by hand so I can retrieve the i18n messages and currentUser() can work.
                // will find out later why this is working this way, if you know why this is not set let me know :)
                Http.Context.current.set(ctx);
                securesocial.core.UserId scalaUserId = getUserIdFromSession(ctx);

                if ( scalaUserId == null ) {
                    if ( Logger.isDebugEnabled() ) {
                        Logger.debug("Anonymous user trying to access : " + ctx.request().uri());
                    }
                    if ( configuration.isApiClient() ) {
                        return forbidden( forbiddenJson() );
                    } else {
                        ctx.flash().put("error", play.i18n.Messages.get("securesocial.loginRequired"));
                        ctx.session().put(ORIGINAL_URL, ctx.request().uri());
                        return redirect(securesocial.controllers.routes.LoginPage.login());
                    }
                } else {
                    SocialUser user = currentUser();
                    if ( user != null ) {
                        ctx.args.put(USER_KEY, user);
                        return delegate.call(ctx);
                    } else {
                        // there is no user in the backing store matching the credentials sent by the client.
                        // we need to remove the credentials from the session
                        if ( configuration.isApiClient() ) {
                            ctx.session().remove(USER_KEY);
                            ctx.session().remove(PROVIDER_KEY);
                            return forbidden( forbiddenJson() );
                        } else {
                            return redirect(securesocial.controllers.routes.LoginPage.logout());
                        }
                    }
                }
            } finally {
                // leave it null as it was before, just in case.H
                Http.Context.current.set(null);
            }
        }
    }


    /**
     * Actions annotated with UserAwareAction get the current user set in the Context.args holder
     * if there's one available.
     */
    @With(UserAwareAction.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UserAware {
    }

    /**
     * An action that puts the current user in the context if there's one available. This is useful in
     * public actions that need to access the user information if there's one logged in.
     */
    public static class UserAwareAction extends Action<UserAware> {
        @Override
        public Result call(Http.Context ctx) throws Throwable {
            SocialUser user = currentUser();
            if ( user != null ) {
                ctx.args.put(USER_KEY, user);
            }
            return delegate.call(ctx);
        }
    }
}

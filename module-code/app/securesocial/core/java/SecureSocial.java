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
import play.api.libs.oauth.ServiceInfo;
import play.libs.Json;
import play.libs.Scala;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import scala.Option;
import scala.util.Either;
import securesocial.core.Authenticator;
import securesocial.core.Identity;
import securesocial.core.IdentityProvider;
import securesocial.core.SecureSocial$;
import securesocial.core.UserService$;
import securesocial.core.providers.utils.RoutesHelper;

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
 *      Identity user = (Identity) ctx().args.get(SecureSocial.USER_KEY);
 *      return ok("Hello " + user.displayName);
 *  }
 */
public class SecureSocial {

    /**
     * The user key
     */
    public static final String USER_KEY = "securesocial.user";

    /**
     * The original url key
     */
    static final String ORIGINAL_URL = "original-url";

    /**
     * An annotation to mark actions as protected by SecureSocial
     * When the user is not logged in the action redirects the browser to the login page.
     *
     * If the isAjaxCall parameter is set to true SecureSocial will return a forbidden error
     * with a json error instead.
     */
    @With(Secured.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SecuredAction {
        /**
         * Specifies whether the action handles an ajax call or not. Default is false.
         * @return
         */
        boolean ajaxCall() default false;

        /**
         * The Authorization implementation that checks if the user is allowed to execute this action.
         * By default, all requests are accepted.
         *
         * @return
         */
        Class<? extends Authorization> authorization() default DummyAuthorization.class;

        /**
         * The parameters that are passed to the Authorization.isAuthorized implementation
         * @return
         */
        String[] params() default {};
    }

    /**
     * Retrieves the authenticator from the request
     *
     * @param ctx the current context
     * @return the Authenticator or null if there isn't one or has expired.
     */
    private static securesocial.core.Authenticator getAuthenticatorFromRequest(Http.Context ctx) {
        Http.Cookie cookie = ctx.request().cookies().get(Authenticator.cookieName());
        Authenticator result = null;

        if ( cookie != null ) {
            Either<Error, Option<Authenticator>> maybeAuthenticator = Authenticator.find(cookie.value());
            if ( maybeAuthenticator.isRight() ) {
                result = Scala.orNull(maybeAuthenticator.right().get());
                if ( result != null && !result.isValid()) {
                    Authenticator.delete(result.id());
                    ctx.response().discardCookie(
                            Authenticator.cookieName(),
                            Authenticator.cookiePath(),
                            Scala.orNull(Authenticator.cookieDomain()),
                            Authenticator.cookieSecure()
                            );
                    result = null;
                }
            }
        }
        return result;
    }

    /**
     * Returns the current user
     *
     * @return a SocialUser or null if there is no current user
     */
     public static Identity currentUser() {
        Authenticator authenticator = getAuthenticatorFromRequest(Http.Context.current());
        return currentUser(authenticator);
     }

    private static Identity currentUser(Authenticator authenticator) {
        Identity result = null;

         if ( authenticator != null ) {
             Option<Identity> optionalIdentity = UserService$.MODULE$.find(authenticator.userId());
             result = Scala.orNull(optionalIdentity);

         }
         return result;
    }

    /**
     * Returns the ServiceInfo needed to sign OAuth1 requests.
     *
     * @param user the user for which the serviceInfo is needed
     * @return The ServiceInfo or null if the user did not use an OAuth1 provider
     */
    public static ServiceInfo serviceInfoFor(Identity user) {
        return Scala.orNull( SecureSocial$.MODULE$.serviceInfoFor(user));
    }

    /**
     * Generates the error json required for ajax calls calls when the
     * user is not authenticated
     *
     * @return
     */
    private static ObjectNode ajaxCallNotAuthenticated() {
        ObjectNode result = Json.newObject();
        result.put("error", "Credentials required");
        return result;
    }

    /**
     * Generates the error json required for ajax calls calls when the
     * user is not authorized to execute the action
     *
     * @return
     */
    private static ObjectNode ajaxCallNotAuthorized() {
        ObjectNode result = Json.newObject();
        result.put("error", "Not authorized");
        return result;
    }

    private static void fixHttpContext(Http.Context ctx) {
        // As of Play 2.0.3:
        // I don't understand why the ctx is not set in the Http.Context thread local variable.
        // I'm setting it by hand so I can retrieve the i18n messages and currentUser() can work.
        // will find out later why this is working this way, if you know why this is not set let me know :)
        // This is looks like a bug, Play should be setting the context properly.
        Http.Context.current.set(ctx);
    }

    /**
     * Protects an action with SecureSocial
     */
    public static class Secured extends Action<SecuredAction> {

        @Override
        public Result call(Http.Context ctx) throws Throwable {
            try {
                fixHttpContext(ctx);
                final Authenticator authenticator = getAuthenticatorFromRequest(ctx);
                final Identity user = authenticator != null ? currentUser(authenticator) : null;
                if ( user == null ) {
                    if ( Logger.isDebugEnabled() ) {
                        Logger.debug("[securesocial] anonymous user trying to access : " + ctx.request().uri());
                    }
                    if ( configuration.ajaxCall() ) {
                        return unauthorized(ajaxCallNotAuthenticated());
                    } else {
                        ctx.flash().put("error", play.i18n.Messages.get("securesocial.loginRequired"));
                        ctx.session().put(ORIGINAL_URL, ctx.request().uri());
                        return redirect(RoutesHelper.login().absoluteURL(ctx.request(), IdentityProvider.sslEnabled()));
                    }
                } else {
                    Authorization authorization = configuration.authorization().newInstance();

                    if ( authorization.isAuthorized(user, configuration.params()) ) {
                        ctx.args.put(USER_KEY, user);
                        touch(authenticator);
                        return delegate.call(ctx);
                    } else {
                        if ( configuration.ajaxCall() ) {
                            return forbidden(ajaxCallNotAuthorized());
                        } else {
                            return redirect(RoutesHelper.notAuthorized());
                        }
                    }
                }
            } finally {
                // leave it null as it was before, just in case.
                Http.Context.current.set(null);
            }
        }
    }

    private static void touch(Authenticator authenticator) {
        Authenticator.save(authenticator.touch());
    }

    /**
     * Actions annotated with UserAwareAction get the current user set in the Context.args holder
     * if there's one available.
     */
    @With(UserAware.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UserAwareAction {
    }

    /**
     * An action that puts the current user in the context if there's one available. This is useful in
     * public actions that need to access the user information if there's one logged in.
     */
    public static class UserAware extends Action<UserAwareAction> {
        @Override
        public Result call(Http.Context ctx) throws Throwable {
            SecureSocial.fixHttpContext(ctx);
            try {
                Authenticator authenticator = getAuthenticatorFromRequest(ctx);
                Identity user = authenticator != null ? currentUser(authenticator): null;

                if ( user != null ) {
                    touch(authenticator);
                    ctx.args.put(USER_KEY, user);
                }
                return delegate.call(ctx);
            } finally {
                // leave it null as it was before, just in case.
                Http.Context.current.set(null);
            }
        }
    }
}

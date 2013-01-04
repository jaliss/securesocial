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
import org.joda.time.DateTime;
import play.Logger;
import play.Play;
import play.api.libs.oauth.ServiceInfo;
import play.libs.Json;
import play.libs.Scala;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import scala.Option;
import securesocial.core.Identity;
import securesocial.core.SecureSocial$;
import securesocial.core.UserId;
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
     * The provider key
     */
    static final String PROVIDER_KEY = "securesocial.provider";

    /**
     * The original url key
     */
    static final String ORIGINAL_URL = "securesocial.originalUrl";

    /**
     * The last time access
     */
    static final String LAST_ACCESS = "securesocial.lastAccess";

    /**
     * The session timeout key in the conf file
     */
    static final String SESSION_KEY = "securesocial.sessionTimeOut";

    /**
     * The default timeout for sessions in minutes
     */
    static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

    /**
     * The timeout specified in the conf file
     */
    static final Integer CUSTOM_SESSION_TIMEOUT = Play.application().configuration().getInt(SESSION_KEY);

    /**
     * The timeout to use (either the default or the custom one specified in the conf file)
     */
    static final int SESSION_TIMEOUT = CUSTOM_SESSION_TIMEOUT != null ? CUSTOM_SESSION_TIMEOUT :
            DEFAULT_SESSION_TIMEOUT_MINUTES;

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
     * Creates a UserId from the session
     *
     * @param ctx the current context
     * @return the UserId or null if there's no user in the session
     */
    private static securesocial.core.UserId getUserIdFromSession(Http.Context ctx) {
        final String user = ctx.session().get(USER_KEY);
        final String provider = ctx.session().get(PROVIDER_KEY);

        UserId result = null;

        if ( user != null && provider != null ) {
            final String dateTimeString = ctx.session().get(LAST_ACCESS);
            final DateTime lastAccess = DateTime.parse(dateTimeString);
            if ( !isSessionExpired(lastAccess) ) {
                result = new UserId(user,provider);
            }
        }
        return result;
    }

    /**
     * Checks if the session has expired
     *
     * @param lastAccess the last access date that came in the session
     * @return  true if the session has expired, false otherwise.
     */
    private static boolean isSessionExpired(DateTime lastAccess) {
        return DateTime.now().isAfter(lastAccess.plusMinutes(SESSION_TIMEOUT));
    }

    /**
     * Returns the current user
     *
     * @return a SocialUser or null if there is no current user
     */
     public static Identity currentUser() {
        Identity result = null;
        UserId userId = getUserIdFromSession(Http.Context.current());

         if ( userId != null ) {
             Option<Identity> optionalIdentity = UserService$.MODULE$.find(userId);
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
                securesocial.core.UserId scalaUserId = getUserIdFromSession(ctx);

                if ( scalaUserId == null ) {
                    if ( Logger.isDebugEnabled() ) {
                        Logger.debug("[securesocial] anonymous user trying to access : " + ctx.request().uri());
                    }
                    if ( configuration.ajaxCall() ) {
                        return unauthorized(ajaxCallNotAuthenticated());
                    } else {
                        ctx.flash().put("error", play.i18n.Messages.get("securesocial.loginRequired"));
                        ctx.session().put(ORIGINAL_URL, ctx.request().uri());
                        return redirect(RoutesHelper.login());
                    }
                } else {
                    Identity user = currentUser();
                    if ( user != null ) {
                        Authorization authorization = configuration.authorization().newInstance();

                        if ( authorization.isAuthorized(user, configuration.params()) ) {
                            ctx.args.put(USER_KEY, user);
                            Result actionResult = delegate.call(ctx);
                            touchSession(ctx);
                            return actionResult;
                        } else {
                            if ( configuration.ajaxCall() ) {
                                return forbidden(ajaxCallNotAuthorized());
                            } else {
                                return redirect(RoutesHelper.notAuthorized());
                            }
                        }
                    } else {
                        // there is no user in the backing store matching the credentials sent by the client.
                        // we need to remove the credentials from the session
                        if ( configuration.ajaxCall() ) {
                            ctx.session().remove(USER_KEY);
                            ctx.session().remove(PROVIDER_KEY);
                            return forbidden( ajaxCallNotAuthenticated() );
                        } else {
                            return redirect(RoutesHelper.logout());
                        }
                    }
                }
            } finally {
                // leave it null as it was before, just in case.
                Http.Context.current.set(null);
            }
        }
    }

    private static void touchSession(Http.Context ctx) {
        ctx.session().put(LAST_ACCESS, DateTime.now().toString());
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
                Identity user = currentUser();
                if ( user != null ) {
                    ctx.args.put(USER_KEY, user);
                }
                Result actionResult = delegate.call(ctx);
                if ( user != null ) {
                    touchSession(ctx);
                }
                return actionResult;
            } finally {
                // leave it null as it was before, just in case.
                Http.Context.current.set(null);
            }
        }
    }
}

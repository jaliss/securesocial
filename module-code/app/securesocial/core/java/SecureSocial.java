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

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.api.templates.Html;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.*;
import scala.Option;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.authenticator.Authenticator;

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
// */
public abstract class SecureSocial extends Controller {

    /**
     * Subclasses need to provide a Runtime Environment for the controller
     */
    public static RuntimeEnvironment env() {
        return (RuntimeEnvironment) Http.Context.current().args.get("securesocial-env");
    }

    protected static void initEnv(RuntimeEnvironment env) throws IllegalAccessException, InstantiationException {
        if ( env() == null ) {
            Http.Context.current().args.put("securesocial-env", env);
        }
    }

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

    protected  static Html notAuthorizedPage(Http.Context ctx) {
        return securesocial.views.html.notAuthorized.render(ctx.lang(), env());
    }

    /**
     * Generates the error json required for ajax calls calls when the
     * user is not authenticated
     *
     * @return
     */
    protected  static Promise<Result> notAuthenticatedResult(Http.Context ctx) {
        Http.Request req = ctx.request();
        Result result;

        if ( req.accepts("text/html")) {
            ctx.flash().put("error", play.i18n.Messages.get("securesocial.loginRequired"));
            ctx.session().put(ORIGINAL_URL, ctx.request().uri());
            result = redirect(env().routes().loginPageUrl(ctx._requestHeader()));
        } else if ( req.accepts("application/json")) {
            ObjectNode node = Json.newObject();
            node.put("error", "Credentials required");
            result = unauthorized(node);
        } else {
            result = unauthorized("Credentials required");
        }
        return Promise.pure(result);
    }

    /**
     * Generates the error json required for ajax calls calls when the
     * user is not authorized to execute the action
     *
     * @return
     */
    protected static Promise<Result> notAuthorizedResult(Http.Context ctx) {
        Http.Request req = ctx.request();
        Result result;

        if ( req.accepts("text/html")) {
            result = forbidden(notAuthorizedPage(ctx));
        } else if ( req.accepts("application/json")) {
            ObjectNode node = Json.newObject();
            node.put("error", "Not authorized");
            result = forbidden(node);
        } else {
            result = forbidden("Not authorized");
        }

        return Promise.pure(result);
    }

    /**
     * Protects an action with SecureSocial
     */
    public static class Secured extends Action<SecuredAction> {
        private RuntimeEnvironment env;

        public Secured(RuntimeEnvironment env) {
            this.env = env;
        }

        private play.Logger.ALogger logger = play.Logger.of("securesocial.core.java.Secured");

        @Override
        public Promise<Result> call(final Http.Context ctx) throws Throwable {
            initEnv(env);
            return F.Promise.wrap(env.authenticatorService().fromRequest(ctx._requestHeader())).flatMap(
                    new F.Function<Option<Authenticator>, Promise<Result>>() {
                        @Override
                        public Promise<Result> apply(Option<Authenticator> authenticatorOption) throws Throwable {
                            if (authenticatorOption.isDefined() && authenticatorOption.get().isValid()) {
                                final Authenticator authenticator = authenticatorOption.get();
                                Object user = authenticator.user();
                                Authorization authorization = configuration.authorization().newInstance();
                                if (authorization.isAuthorized(user, configuration.params())) {
                                    return F.Promise.wrap(authenticator.touch()).flatMap(new F.Function<Authenticator, Promise<Result>>() {
                                        @Override
                                        public Promise<Result> apply(Authenticator touched) throws Throwable {
                                            ctx.args.put(USER_KEY, touched.user());
                                            return F.Promise.wrap(touched.touching(ctx)).flatMap(new F.Function<scala.runtime.BoxedUnit, Promise<Result>>() {
                                                @Override
                                                public Promise<Result> apply(scala.runtime.BoxedUnit unit) throws Throwable {
                                                    return delegate.call(ctx);
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    return notAuthorizedResult(ctx);
                                }
                            } else {
                                if (authenticatorOption.isDefined()) {
                                    return F.Promise.wrap(authenticatorOption.get().discarding(ctx)).flatMap(
                                            new F.Function<Authenticator, Promise<Result>>() {
                                                @Override
                                                public Promise<Result> apply(Authenticator authenticator) throws Throwable {
                                                    return notAuthenticatedResult(ctx);
                                                }
                                            }
                                    );
                                }
                                return notAuthenticatedResult(ctx);
                            }
                        }
                    }
            );
        }
    }

    /**
     * Actions annotated with UserAwareAction get the current user set in the Context.args holder
     * if there's one available.
     */
    @With(SecureSocial.UserAware.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UserAwareAction {
    }

    /**
     * An action that puts the current user in the context if there's one available. This is useful in
     * public actions that need to access the user information if there's one logged in.
     */
    public static class UserAware extends Action<UserAwareAction> {
        RuntimeEnvironment env;
        public UserAware(RuntimeEnvironment env) {
            this.env = env;
        }
        @Override
        public Promise<Result> call(final Http.Context ctx) throws Throwable {
            initEnv(env);
            return F.Promise.wrap(env.authenticatorService().fromRequest(ctx._requestHeader())).flatMap(
            new F.Function<Option<Authenticator>, Promise<Result>>() {
                @Override
                public Promise<Result> apply(Option<Authenticator> authenticatorOption) throws Throwable {
                    if (authenticatorOption.isDefined() && authenticatorOption.get().isValid()) {
                        Authenticator authenticator = authenticatorOption.get();
                        return F.Promise.wrap(authenticator.touch()).flatMap(new F.Function<Authenticator, Promise<Result>>() {
                            @Override
                            public Promise<Result> apply(Authenticator touched) throws Throwable {
                                ctx.args.put(USER_KEY, touched.user());
                                return F.Promise.wrap(touched.touching(ctx)).flatMap(new F.Function<scala.runtime.BoxedUnit, Promise<Result>>() {
                                    @Override
                                    public Promise<Result> apply(scala.runtime.BoxedUnit unit) throws Throwable {
                                        return delegate.call(ctx);
                                    }
                                });
                            }
                        });
                    } else {
                        return delegate.call(ctx);
                    }
                }
            });
        }
    }
}

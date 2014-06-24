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
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.authenticator.Authenticator;

/**
 * Protects an action with SecureSocial
 *
 * Sample usage:
 *
 *  @SecuredAction
 *  public static Result index() {
 *      User user = (User) ctx().args.get(SecureSocial.USER_KEY);
 *      return ok("Hello " + user.displayName);
 *  }
 */
public class Secured extends Action<SecuredAction> {

    private RuntimeEnvironment env;
    private Authorization authorizationInstance;
    private SecuredActionResponses responses;

    public Secured(RuntimeEnvironment<?> env) throws Throwable {
        this.env = env;
    }

    static void initEnv(RuntimeEnvironment env) throws IllegalAccessException, InstantiationException {
        if ( SecureSocial.env() == null ) {
            Http.Context.current().args.put("securesocial-env", env);
        }
    }


    @Override
    public F.Promise<Result> call(final Http.Context ctx) throws Throwable {
        initEnv(env);
        authorizationInstance = configuration.authorization().newInstance();
        responses = configuration.responses().newInstance();
        return F.Promise.wrap(env.authenticatorService().fromRequest(ctx._requestHeader())).flatMap(
                new F.Function<Option<Authenticator>, F.Promise<Result>>() {
                    @Override
                    public F.Promise<Result> apply(Option<Authenticator> authenticatorOption) throws Throwable {
                        if (authenticatorOption.isDefined() && authenticatorOption.get().isValid()) {
                            final Authenticator authenticator = authenticatorOption.get();
                            Object user = authenticator.user();
                            if (authorizationInstance.isAuthorized(user, configuration.params())) {
                                return F.Promise.wrap(authenticator.touch()).flatMap(new F.Function<Authenticator, F.Promise<Result>>() {
                                    @Override
                                    public F.Promise<Result> apply(Authenticator touched) throws Throwable {
                                        ctx.args.put(SecureSocial.USER_KEY, touched.user());
                                        return F.Promise.wrap(touched.touching(ctx)).flatMap(new F.Function<scala.runtime.BoxedUnit, F.Promise<Result>>() {
                                            @Override
                                            public F.Promise<Result> apply(scala.runtime.BoxedUnit unit) throws Throwable {
                                                return delegate.call(ctx);
                                            }
                                        });
                                    }
                                });
                            } else {
                                return responses.notAuthorizedResult(ctx);
                            }
                        } else {
                            if (authenticatorOption.isDefined()) {
                                return F.Promise.wrap(authenticatorOption.get().discarding(ctx)).flatMap(
                                        new F.Function<Authenticator, F.Promise<Result>>() {
                                            @Override
                                            public F.Promise<Result> apply(Authenticator authenticator) throws Throwable {
                                                return responses.notAuthenticatedResult(ctx);
                                            }
                                        }
                                );
                            }
                            return responses.notAuthenticatedResult(ctx);
                        }
                    }
                }
        );
    }
}
/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package securesocial.core.java;

import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.authenticator.Authenticator;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static scala.compat.java8.FutureConverters.toJava;

/**
 * An action that puts the current user in the context if there's one available. This is useful in
 * public actions that need to access the user information if there's one logged in.
 * <p>
 * Sample usage:
 *
 * @UserAwareAction public static Result index() {
 * User user = (User) ctx().args.get(SecureSocial.USER_KEY);
 * String name = user == null ? "guest" : user.displayName;
 * return ok("Hello " + name);
 * }
 * @see securesocial.core.java.UserAwareAction
 */
public class UserAware extends Action<UserAwareAction> {
    RuntimeEnvironment env;

    @Inject
    public UserAware(RuntimeEnvironment env) throws Throwable {
        this.env = env;
    }

    @Override
    public CompletionStage<Result> call(final Http.Context ctx) {
        Exception exeption;
        try {
            Secured.initEnv(env);
            return toJava(env.authenticatorService().fromRequest(ctx._requestHeader())).thenComposeAsync(
                    new Function<Option<Authenticator<Object>>, CompletionStage<Result>>() {
                        @Override
                        public CompletionStage<Result> apply(Option<Authenticator<Object>> authenticatorOption) {
                            if (authenticatorOption.isDefined() && authenticatorOption.get().isValid()) {
                                Authenticator authenticator = authenticatorOption.get();
                                return toJava(authenticator.touch()).thenComposeAsync(new Function<Authenticator, CompletionStage<Result>>() {
                                    @Override
                                    public CompletionStage<Result> apply(Authenticator touched) {
                                        ctx.args.put(SecureSocial.USER_KEY, touched.user());
                                        return toJava(touched.touching(ctx)).thenComposeAsync(new Function<scala.runtime.BoxedUnit, CompletionStage<Result>>() {
                                            @Override
                                            public CompletionStage<Result> apply(scala.runtime.BoxedUnit unit) {
                                                return delegate.call(ctx);
                                            }
                                        });
                                    }
                                });
                            } else {
                                return delegate.call(ctx);
                            }
                        }
                    }

            );
        } catch (IllegalAccessException | InstantiationException e) {
            exeption = e;
        }
        throw new RuntimeException(exeption);
    }
}
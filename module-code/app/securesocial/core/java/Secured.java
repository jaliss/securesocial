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

import play.libs.concurrent.HttpExecution;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Option;
import scala.concurrent.ExecutionContextExecutor;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.authenticator.Authenticator;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static play.libs.concurrent.HttpExecution.defaultContext;
import static scala.compat.java8.FutureConverters.toJava;

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
    private static final String ENVIRONMENT_KEY = "securesocial-env";

    @Inject
    public Secured(RuntimeEnvironment env) throws Throwable {
        this.env = env;
    }

    static void initEnv(RuntimeEnvironment env) throws IllegalAccessException, InstantiationException {
        if ( SecureSocial.env() == null ) {
            Http.Context.current().args.put(ENVIRONMENT_KEY, env);
        }
    }

    static void clearEnv() {
        Http.Context.current().args.remove(ENVIRONMENT_KEY);
    }


    @Override
    public CompletionStage<Result> call(final Http.Context ctx) {
        try {
            initEnv(env);
            authorizationInstance = configuration.authorization().newInstance();
            responses = configuration.responses().newInstance();
            return toJava(env.authenticatorService().fromRequest(ctx._requestHeader()))
                    .thenComposeAsync(new CheckAuthenticator(ctx), HttpExecution.defaultContext())
                    .whenComplete((result, ex) -> Secured.clearEnv());
        } catch (Throwable t) {
            CompletableFuture<Result> failedResult = new CompletableFuture<>();
            failedResult.completeExceptionally(t);
            return failedResult;
        }
    }

    class CheckAuthenticator implements Function<Option<Authenticator<Object>>, CompletionStage<Result>> {
        private final Http.Context ctx;

        CheckAuthenticator(Http.Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public CompletionStage<Result> apply(Option<Authenticator<Object>> authenticatorOption) {
            ExecutionContextExecutor executor = HttpExecution.defaultContext();

            if (authenticatorOption.isDefined() && authenticatorOption.get().isValid()) {
                final Authenticator<Object> authenticator = authenticatorOption.get();
                Object user = authenticator.user();
                if (authorizationInstance.isAuthorized(user, configuration.params())) {
                    return toJava(authenticator.touch())
                            .thenComposeAsync(new InvokeDelegate(ctx, delegate), executor);
                } else {
                    return responses.notAuthorizedResult(ctx);
                }
            } else {
                if (authenticatorOption.isDefined()) {
                    return toJava(authenticatorOption.get().discarding(ctx))
                            .thenComposeAsync(boxedUnit -> responses.notAuthenticatedResult(ctx), executor);
                }
                return responses.notAuthenticatedResult(ctx);
            }
        }
    }
}
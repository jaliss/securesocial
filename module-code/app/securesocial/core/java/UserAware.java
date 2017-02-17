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

import securesocial.core.RuntimeEnvironment;
import securesocial.core.authenticator.Authenticator;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import scala.concurrent.ExecutionContextExecutor;
import static scala.compat.java8.FutureConverters.toJava;

/**
 * An action that puts the current user in the context if there's one available. This is useful in
 * public actions that need to access the user information if there's one logged in.
 *
 * Sample usage:
 *
 *  @UserAwareAction
 *  public static Result index() {
 *      User user = (User) ctx().args.get(SecureSocial.USER_KEY);
 *      String name = user == null ? "guest" : user.displayName;
 *      return ok("Hello " + name);
 *  }
 * @see securesocial.core.java.UserAwareAction
 */
public class UserAware extends Action<UserAwareAction> {
    private RuntimeEnvironment env;

    @Inject
    public UserAware(RuntimeEnvironment env) throws Throwable {
        this.env = env;
    }

    @Override
    public CompletionStage<Result> call(final Http.Context ctx)  {
        try {
            Secured.initEnv(env);
            ExecutionContextExecutor executor = HttpExecution.defaultContext();
            return toJava(env.authenticatorService().fromRequest(ctx._requestHeader()))
                    .thenComposeAsync(authenticatorOption -> {
                        if (authenticatorOption.isDefined() && authenticatorOption.get().isValid()) {
                            Authenticator<Object> authenticator = authenticatorOption.get();
                            return toJava(authenticator.touch())
                                    .thenComposeAsync(new InvokeDelegate(ctx, delegate), executor);
                        } else {
                            return delegate.call(ctx);
                        }
                    }, executor)
                    .whenComplete((result, ex) -> Secured.clearEnv());
        } catch (Throwable t) {
            CompletableFuture<Result> failedResult = new CompletableFuture<>();
            failedResult.completeExceptionally(t);
            return failedResult;
        }
    }
}
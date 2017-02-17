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

import play.api.mvc.RequestHeader;
import play.libs.concurrent.HttpExecution;
import play.mvc.Http;
import scala.Option;
import scala.concurrent.ExecutionContext;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.SecureSocial$;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import static scala.compat.java8.FutureConverters.toJava;
/**
*
*/
public class SecureSocial {
    /**
     * The user key
     */
    public static final String USER_KEY = "securesocial.user";

    /**
     * The original url key
     */
    public static final String ORIGINAL_URL = "original-url";

    /**
     * Returns the current environment.
     */
    public static RuntimeEnvironment env() {
        return (RuntimeEnvironment) Http.Context.current().args.get("securesocial-env");
    }

    /**
     * Returns the current user. Invoke this only if you are executing code
     * without a SecuredAction or UserAware action available (eg: using WebSockets). For other cases this
     * should not be needed.
     *
     * @param env the environment
     * @return the current user object or null if there isn't one available
     */
    public static CompletionStage<Object> currentUser(RuntimeEnvironment env) {
        return currentUser(env, HttpExecution.defaultContext());
    }

    /**
     * Returns the current user. Invoke this only if you are executing code
     * without a SecuredAction or UserAware action available (eg: using WebSockets). For other cases this
     * should not be needed.
     *
     * @param env the environment
     * @param executor an ExecutionContext
     * @return the current user object or null if there isn't one available
     */
    public static CompletionStage<Object> currentUser(RuntimeEnvironment env, ExecutionContext executor) {
        RequestHeader requestHeader = Http.Context.current()._requestHeader();
        if (requestHeader == null || env == null) {
            return CompletableFuture.completedFuture(null);
        } else {
            scala.concurrent.Future<Option<Object>> scalaFuture = SecureSocial$.MODULE$.currentUser(requestHeader, env, executor);
            return toJava(scalaFuture).thenApply(userOption -> userOption.isDefined() ? userOption.get() : null);
        }
    }
}

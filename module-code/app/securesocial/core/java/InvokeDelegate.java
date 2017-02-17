/**
 * Copyright 2012-2017 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
import scala.concurrent.ExecutionContextExecutor;
import securesocial.core.authenticator.Authenticator;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static scala.compat.java8.FutureConverters.toJava;

class InvokeDelegate implements Function<Authenticator<Object>, CompletionStage<Result>> {
    private final Http.Context ctx;
    private final Action<?> delegate;

    InvokeDelegate(Http.Context ctx, Action<?> delegate) {
        this.ctx = ctx;
        this.delegate = delegate;
    }

    @Override
    public CompletionStage<Result> apply(Authenticator<Object> authenticator) {
        ctx.args.put(SecureSocial.USER_KEY, authenticator.user());
        return toJava(authenticator.touching(ctx))
                .thenComposeAsync(boxedUnit -> delegate.call(ctx), HttpExecution.defaultContext());
    }
}
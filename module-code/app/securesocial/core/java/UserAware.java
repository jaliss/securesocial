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
import play.mvc.SimpleResult;
import scala.Option;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.authenticator.Authenticator;

import static play.libs.F.Promise;

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
    RuntimeEnvironment env;

    @Override
    public F.Promise<SimpleResult> call(final Http.Context ctx) throws Throwable {
        Secured.initEnv(env);
        return (F.Promise<SimpleResult>) F.Promise.wrap(env.authenticatorService().fromRequest(ctx._requestHeader())).flatMap(
        new F.Function<Option<Authenticator>, Promise<SimpleResult>>() {
            @Override
            public F.Promise<SimpleResult> apply(Option<Authenticator> authenticatorOption) throws Throwable {
                if (authenticatorOption.isDefined() && authenticatorOption.get().isValid()) {
                    Authenticator authenticator = authenticatorOption.get();
                    return F.Promise.wrap(authenticator.touch()).flatMap(new F.Function<Authenticator, Promise<SimpleResult>>() {
                        @Override
                        public Promise<SimpleResult> apply(Authenticator touched) throws Throwable {
                            ctx.args.put(SecureSocial.USER_KEY, touched.user());
                            return F.Promise.wrap(touched.touching(ctx)).flatMap(new F.Function<scala.runtime.BoxedUnit, Promise<SimpleResult>>() {
                                @Override
                                public Promise<SimpleResult> apply(scala.runtime.BoxedUnit unit) throws Throwable {
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
/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

/**
 * The default responses sent when the invoker is not authenticated or authorized to execute a protected
 * action.
 *
 * @see securesocial.core.java.SecuredActionResponses
 */
public class DefaultSecuredActionResponses extends Controller implements SecuredActionResponses {
    public Html notAuthorizedPage(Http.Context ctx) {
        return securesocial.views.html.notAuthorized.render(ctx._requestHeader(), ctx.lang(), SecureSocial.env());
    }

    public F.Promise<Result> notAuthenticatedResult(Http.Context ctx) {
        Http.Request req = ctx.request();
        Result result;

        if ( req.accepts("text/html")) {
            ctx.flash().put("error", play.i18n.Messages.get("securesocial.loginRequired"));
            ctx.session().put(SecureSocial.ORIGINAL_URL, ctx.request().uri());
            result = redirect(SecureSocial.env().routes().loginPageUrl(ctx._requestHeader()));
        } else if ( req.accepts("application/json")) {
            ObjectNode node = Json.newObject();
            node.put("error", "Credentials required");
            result = unauthorized(node);
        } else {
            result = unauthorized("Credentials required");
        }
        return F.Promise.pure(result);
    }

    public F.Promise<Result> notAuthorizedResult(Http.Context ctx) {
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

        return F.Promise.pure(result);
    }
}

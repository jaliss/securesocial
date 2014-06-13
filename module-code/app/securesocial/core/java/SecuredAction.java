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

import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark actions as protected by SecureSocial
 *
 * @see securesocial.core.java.Secured
 */
@With(Secured.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredAction {
    /**
     * The Authorization implementation that checks if the user is allowed to execute this action.
     * By default, all requests are accepted.
     */
    Class<? extends Authorization> authorization() default DummyAuthorization.class;

    /**
     * The responses sent when the invoker is not authorized or authenticated
     *
     * @see securesocial.core.java.DefaultSecuredActionResponses
     */
    Class<? extends SecuredActionResponses> responses() default DefaultSecuredActionResponses.class;

    /**
     * The parameters that are passed to the Authorization.isAuthorized implementation
     */
    String[] params() default {};
}
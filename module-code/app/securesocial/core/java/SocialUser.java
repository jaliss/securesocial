/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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

import play.libs.Scala;
import scala.Option;

/**
 * A class representing a connected user and its authentication details.
 */
public class SocialUser {
    /**
     * The user id
     */
    public UserId id;

    /**
     * The user display name.
     */
    public String displayName;

    /**
     * The user's email (some providers can't provide the email, eg: twitter)
     */
    public String email;

    /**
     * A URL pointing to the user avatar
     */
    public String avatarUrl;

    /**
     * The method that was used to authenticate the user.
     */
    public AuthenticationMethod authMethod;

    /**
     * The OAuth1 details required to make calls to the API for OAUTH1 users
     * (available when authMethod is OAUTH1)
     *
     */
    public OAuth1Info oAuth1Info;
    public OAuth2Info oAuth2Info;

    public static SocialUser fromScala(securesocial.core.SocialUser scalaUser) {
        SocialUser user = new SocialUser();
        user.id = new UserId();
        user.id.id = scalaUser.id().id();
        user.id.provider = scalaUser.id().providerId();
        user.displayName = scalaUser.displayName();
        user.avatarUrl = Scala.orNull(scalaUser.avatarUrl());
        user.email = Scala.orNull(scalaUser.email());
        user.authMethod = AuthenticationMethod.fromScala(scalaUser.authMethod());

        if ( scalaUser.oAuth1Info().isDefined() ) {
            user.oAuth1Info = OAuth1Info.fromScala(scalaUser.oAuth1Info().get());
        }

        if ( scalaUser.oAuth2Info().isDefined() ) {
            user.oAuth2Info = OAuth2Info.fromScala(scalaUser.oAuth2Info().get());
        }
        return user;
    }

    public securesocial.core.SocialUser toScala() {
        securesocial.core.UserId userId = securesocial.core.UserId$.MODULE$.apply(id.id, id.provider);
        return securesocial.core.SocialUser$.MODULE$.apply(userId,
                displayName,
                Scala.Option(email),
                Scala.Option(avatarUrl),
                AuthenticationMethod.toSala(authMethod),
                optionalOAuth1Info(),
                optionalOAuth2Info()
        );
    }

    private Option<securesocial.core.OAuth1Info> optionalOAuth1Info() {
        securesocial.core.OAuth1Info scalaInfo = null;

        if ( oAuth1Info != null ) {
            // serviceInfo does not need conversion because it's a Scala object already.
            scalaInfo = securesocial.core.OAuth1Info$.MODULE$.apply(oAuth1Info.serviceInfo, oAuth1Info.token, oAuth1Info.secret);
        }
        return Scala.Option(scalaInfo);
    }

    private Option<securesocial.core.OAuth2Info> optionalOAuth2Info() {
        securesocial.core.OAuth2Info scalaInfo = null;

        if ( oAuth2Info != null ) {
            scalaInfo = securesocial.core.OAuth2Info$.MODULE$.apply(
                    oAuth2Info.accessToken,
                    Scala.Option(oAuth2Info.tokenType),
                    Scala.Option((Object)oAuth2Info.expiresIn),
                    Scala.Option(oAuth2Info.refreshToken)
            );
        }
        return Scala.Option(scalaInfo);
    }
}

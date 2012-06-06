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
package securesocial.core

import play.api.libs.oauth.ServiceInfo

/**
 *
 */
case class SocialUser(id: UserId, displayName: String, email: Option[String], avatarUrl: Option[String], authMethod: AuthenticationMethod, oAuth1Info: Option[OAuth1Info] = None, oAuth2Info: Option[OAuth2Info] = None)
case class UserId(id: String, providerId: String)
case class OAuth1Info(serviceInfo: ServiceInfo, token: String, secret: String)
case class OAuth2Info(accessToken: String, tokenType: Option[String] = None, expiresIn: Option[Int] = None, refreshToken: Option[String] = None)

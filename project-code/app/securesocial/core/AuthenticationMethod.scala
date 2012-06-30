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

/**
 * A class representing an authentication method
 */
case class AuthenticationMethod(method: String)

/**
 * Authentication methods used by the identity providers
 */
object AuthenticationMethod {
  val OAuth1 = AuthenticationMethod("oauth1")
  val OAuth2 = AuthenticationMethod("oauth2")
  val OpenId = AuthenticationMethod("openId")
  val UserPassword = AuthenticationMethod("userPassword")
}


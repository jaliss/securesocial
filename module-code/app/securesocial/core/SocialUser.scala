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
 * This trait represents a user.  Using this trait you to return you can return your own object from the
 * UserService.find methods if you need to instead of returning a SocialUser.
 *
 * In your controller actions you can then convert this Identity to your own class using pattern matching in Scala
 * or a cast in Java.
 *
 * Important: your controllers will receive the instance you created, but this won't work the same for the
 * UserService.save method.  In that case, SecureSocial will pass an instance created by itself (a SocialUser) so
 * do not try to cast the Identity to your own model within your method implementation.
 *
 */
trait Identity {
  def identityId: IdentityId
  def firstName: String
  def lastName: String
  def fullName: String
  def email: Option[String]
  def avatarUrl: Option[String]
  def authMethod: AuthenticationMethod
  def oAuth1Info: Option[OAuth1Info]
  def oAuth2Info: Option[OAuth2Info]
  def passwordInfo: Option[PasswordInfo]
}

/**
 * An implementation of Identity.  Used by SecureSocial to gather user information when users sign up and/or sign in.
 */
case class SocialUser(identityId: IdentityId, firstName: String, lastName: String, fullName: String, email: Option[String],
                      avatarUrl: Option[String], authMethod: AuthenticationMethod,
                      oAuth1Info: Option[OAuth1Info] = None,
                      oAuth2Info: Option[OAuth2Info] = None,
                      passwordInfo: Option[PasswordInfo] = None) extends Identity

object SocialUser {
  def apply(i: Identity): SocialUser = {
    SocialUser(
      i.identityId, i.firstName, i.lastName, i.fullName,
      i.email, i.avatarUrl, i.authMethod, i.oAuth1Info,
      i.oAuth2Info, i.passwordInfo
    )
  }
}

/**
 * The ID of an Identity
 *
 * @param userId the user id on the provider the user came from (eg: twitter, facebook)
 * @param providerId the provider used to sign in
 */
case class IdentityId(userId: String, providerId: String)

/**
 * The OAuth 1 details
 *
 * @param token the token
 * @param secret the secret
 */
case class OAuth1Info(token: String, secret: String)

/**
 * The Oauth2 details
 *
 * @param accessToken the access token
 * @param tokenType the token type
 * @param expiresIn the number of seconds before the token expires
 * @param refreshToken the refresh token
 */
case class OAuth2Info(accessToken: String, tokenType: Option[String] = None,
                      expiresIn: Option[Int] = None, refreshToken: Option[String] = None)

/**
 * The password details
 *
 * @param hasher the id of the hasher used to hash this password
 * @param password the hashed password
 * @param salt the optional salt used when hashing
 */
case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)

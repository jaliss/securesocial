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
package securesocial.core.providers

import securesocial.core._
import play.api.libs.oauth.{RequestToken, OAuthCalculator}
import play.api.libs.ws.WS
import play.api.{Application, Logger}
import LinkedInProvider._


/**
 * A LinkedIn Provider
 */
class LinkedInProvider(application: Application) extends OAuth1Provider(application) {


  override def id = LinkedInProvider.LinkedIn

  override def fillProfile(user: SocialUser): SocialUser = {
    val oauthInfo = user.oAuth1Info.get
    val promise = WS.url(LinkedInProvider.Api).sign(OAuthCalculator(SecureSocial.serviceInfoFor(user).get.key,
      RequestToken(oauthInfo.token, oauthInfo.secret))).get()

     try {
       val response = awaitResult(promise)
       val me = response.json
       (me \ ErrorCode).asOpt[Int] match {
         case Some(error) => {
           val message = (me \ Message).asOpt[String]
           val requestId = (me \ RequestId).asOpt[String]
           val timestamp = (me \ Timestamp).asOpt[String]
           Logger.error(
             "Error retrieving information from LinkedIn. Error code: %s, requestId: %s, message: %s, timestamp: %s"
               format(error, message, requestId, timestamp)
           )
           throw new AuthenticationException()
         }
         case _ => {
           val userId = (me \ Id).as[String]
           val firstName = (me \ FirstName).asOpt[String].getOrElse("")
           val lastName = (me \ LastName).asOpt[String].getOrElse("")
           val fullName = (me \ FormattedName).asOpt[String].getOrElse("")
           val avatarUrl = (me \ PictureUrl).asOpt[String]

           SocialUser(user).copy(
             identityId = IdentityId(userId, id),
             firstName = firstName,
             lastName = lastName,
             fullName= fullName,
             avatarUrl = avatarUrl
           )
         }
       }
     } catch {
       case e: Exception => {
         Logger.error("[securesocial] error retrieving profile information from LinkedIn", e)
         throw new AuthenticationException()
       }
     }
  }
}

object LinkedInProvider {
  val Api = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,formatted-name,picture-url)?format=json"
  val LinkedIn = "linkedin"
  val ErrorCode = "errorCode"
  val Message = "message"
  val RequestId = "requestId"
  val Timestamp = "timestamp"
  val Id = "id"
  val FirstName = "firstName"
  val LastName = "lastName"
  val FormattedName = "formattedName"
  val PictureUrl = "pictureUrl"

}

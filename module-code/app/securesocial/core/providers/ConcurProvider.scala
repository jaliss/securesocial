/**
 * Copyright 2012-2014 Andreas Fuerer (afu at nezasa dot com) - twitter: @andreasfuerer
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

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.joda.time.DateTime
import org.joda.time.Seconds
import org.joda.time.format.DateTimeFormat

import play.api.Play.current
import play.api.http.HeaderNames
import play.api.libs.ws.Response
import play.api.libs.ws.WS
import play.api.mvc.Request
import securesocial.core.AuthenticationException
import securesocial.core.BasicProfile
import securesocial.core.OAuth2Client
import securesocial.core.OAuth2Constants
import securesocial.core.OAuth2Info
import securesocial.core.OAuth2Provider
import securesocial.core.services.CacheService
import securesocial.core.services.RoutesService

/**
 * A Concur OAuth2 Provider
 * 
 * For the documentation of Concur’s OAuth2 implementation please refer to 
 * https://developer.concur.com/api-documentation/oauth-20-0/web-flow
 * 
 * Unfortunately, Concur does not implement the exact OAuth2 specification.
 * It differs in two main points:
 *  - getAccessToken uses a HTTP GET request (instead of HTTP POST as specified
 *    in http://tools.ietf.org/html/rfc6749#section-3.2)
 *  - the access token response is delivered in XML (instead of JSON as 
 *    specified in http://tools.ietf.org/html/rfc6749#section-5.1)
 */
class ConcurProvider(routesService: RoutesService,
                     cacheService: CacheService,
                     client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService)
{
  /** formatter used to parse the expiration date returned from Concur */
  private val ExpirationDateFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss a");

  override val id = ConcurProvider.Concur

  /**
   * Unfortunately, Concur does not stick to the OAuth2 spec saying that a HTTP POST must be
   * used to get the access token. Instead, a HTTP GET is used in their implementation.
   */
  override def getAccessToken[A](code: String)(implicit request: Request[A], ec: ExecutionContext): Future[OAuth2Info] = {
    val url = settings.accessTokenUrl + "?" + OAuth2Constants.Code + "=" + code + "&" + 
      OAuth2Constants.ClientId + "=" + settings.clientId + "&" + 
      OAuth2Constants.ClientSecret + "=" + settings.clientSecret
    if ( logger.isDebugEnabled ) {
      logger.debug("[securesocial] accessTokenUrl = %s".format(settings.accessTokenUrl))
    }
    val call = WS.url(url).get()
    try {
      call.map { response => buildInfo(response) }
    } catch {
      case e: Exception => {
        logger.error("[securesocial] error trying to get an access token for provider %s".format(id), e)
        throw new AuthenticationException()
      }
    }
  }
  
  /**
   * Concur does not return a JSON structure, but uses an XML structure.
   */
  override def buildInfo(response: Response): OAuth2Info = {
      val xml = response.xml
      if ( logger.isDebugEnabled ) {
        logger.debug("[securesocial] got xml back [" + xml + "]")
      }
      OAuth2Info(
        (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.Token).headOption.map(_.text).getOrElse(""),
        (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.TokenType).headOption.map(_.text),
        (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.ExpirationDate).headOption.map(v => {
          Seconds.secondsBetween(DateTime.now(), ExpirationDateFormatter.parseDateTime(v.text)).getSeconds()
        }),
        (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.RefreshToken).headOption.map(_.text)
      )
  }

  override def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    val promise = WS.url(ConcurProvider.UserProfileApi).withHeaders(
      HeaderNames.AUTHORIZATION -> "OAuth %s".format(accessToken),
      HeaderNames.CONTENT_TYPE -> "application/xml"
    ).get()
    try {
      WS.url(ConcurProvider.UserProfileApi).withHeaders(
        HeaderNames.AUTHORIZATION -> "OAuth %s".format(accessToken),
        HeaderNames.CONTENT_TYPE -> "application/xml"
      ).get().map(response => {
        val xml = response.xml
        if ( logger.isDebugEnabled ) {
          logger.debug("[securesocial] got xml back [" + xml + "]")
        } 
        (xml \\ ConcurProvider.Error).headOption match {
          case Some(error) =>
            val message = (error \\ ConcurProvider.Message).headOption.map(_.text).getOrElse("undefined error message")
            val errorId = (error \\ ConcurProvider.Id).headOption.map(_.text).getOrElse("undefined")
            logger.error("[securesocial] error retrieving profile information from Concur. Error message = '%s', id = '%s'"
              .format(message, errorId))
            throw new AuthenticationException()
          case _ =>
            val me = (xml \\ ConcurProvider.UserProfile)
            val userId = (me \\ ConcurProvider.LoginId).headOption.map(_.text).get
            val firstName = (me \\ ConcurProvider.FirstName).headOption.map(_.text)
            val lastName = (me \\ ConcurProvider.LastName).headOption.map(_.text)
            val email = (me \\ ConcurProvider.FirstName).headOption.map(_.text)
            val fullName = firstName match {
              case Some(n) => Some(n + " " + lastName.getOrElse(""))
              case None => lastName
            }
            BasicProfile(id, userId, firstName, lastName, fullName, email, None, authMethod, oAuth2Info = Some(info))
        }
      })
    } catch {
      case e: Exception => {
        logger.error( "[securesocial] error retrieving profile information from Concur", e)
        throw new AuthenticationException()
      }
    }
  }
}

object ConcurProvider {
  val Concur = "concur"
  
  val UserProfileApi = "https://www.concursolutions.com/api/user/v1.0/User/"
  val AccessToken = "Access_Token"
  val Token = "Token"
  val TokenType = "Token_Typed"
  val ExpirationDate = "Expiration_date"
  val RefreshToken = "Refresh_Token"
  val Error = "Error"
  val Message = "Message"
  val Id = "Id"
  val UserProfile = "UserProfile"
  val LoginId = "LoginId"
  val FirstName = "FirstName"
  val LastName = "LastName"
  val EmailAddress = "EmailAddress"
  
}
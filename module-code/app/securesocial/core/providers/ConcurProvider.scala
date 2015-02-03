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

import org.joda.time.{ DateTime, Seconds }
import org.joda.time.format.DateTimeFormat
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import play.api.mvc.Request
import securesocial.core.{ AuthenticationException, BasicProfile, OAuth2Client, OAuth2Constants, OAuth2Info, OAuth2Provider }
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.Node

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
    extends OAuth2Provider(routesService, client, cacheService) {
  /** formatter used to parse the expiration date returned from Concur */
  private val ExpirationDateFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss a")

  override val id = ConcurProvider.Concur

  /**
   * Unfortunately, Concur does not stick to the OAuth2 spec saying that a HTTP POST must be
   * used to get the access token. Instead, a HTTP GET is used in their implementation.
   */
  override def getAccessToken[A](code: String)(implicit request: Request[A]): Future[OAuth2Info] = {
    val url = settings.accessTokenUrl + "?" + OAuth2Constants.Code + "=" + code + "&" +
      OAuth2Constants.ClientId + "=" + settings.clientId + "&" +
      OAuth2Constants.ClientSecret + "=" + settings.clientSecret
    logger.debug("[securesocial] accessTokenUrl = %s".format(settings.accessTokenUrl))
    client.httpService.url(url).get().map { response =>
      buildInfo(response)
    } recover {
      case e =>
        logger.error("[securesocial] error trying to get an access token for provider %s".format(id), e)
        throw new AuthenticationException()
    }
  }

  /**
   * Concur does not return a JSON structure, but uses an XML structure.
   */
  override def buildInfo(response: WSResponse): OAuth2Info = {
    val xml = response.xml
    logger.debug("[securesocial] got xml back [" + maskSensitiveInformation(xml) + "]")
    OAuth2Info(
      (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.Token).headOption.map(_.text).getOrElse(""),
      (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.TokenType).headOption.map(_.text),
      (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.ExpirationDate).headOption.map(v => {
        Seconds.secondsBetween(DateTime.now(), ExpirationDateFormatter.parseDateTime(v.text)).getSeconds
      }),
      (xml \\ ConcurProvider.AccessToken \\ ConcurProvider.RefreshToken).headOption.map(_.text)
    )
  }

  override def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    val accessToken = info.accessToken
    client.httpService.url(ConcurProvider.UserProfileApi).withHeaders(
      HeaderNames.AUTHORIZATION -> "OAuth %s".format(accessToken),
      HeaderNames.CONTENT_TYPE -> "application/xml"
    ).get().map { response =>
        val xml = response.xml
        logger.debug("[securesocial] got xml back [" + xml + "]")
        (xml \\ ConcurProvider.Error).headOption match {
          case Some(error) =>
            val message = (error \\ ConcurProvider.Message).headOption.map(_.text).getOrElse("undefined error message")
            val errorId = (error \\ ConcurProvider.Id).headOption.map(_.text).getOrElse("undefined")
            logger.error("[securesocial] error retrieving profile information from Concur. Error message = '%s', id = '%s'"
              .format(message, errorId))
            throw new AuthenticationException()
          case _ =>
            val me = xml \\ ConcurProvider.UserProfile
            val userId = (me \\ ConcurProvider.LoginId).headOption.map(_.text).get
            val firstName = (me \\ ConcurProvider.FirstName).headOption.map(_.text)
            val lastName = (me \\ ConcurProvider.LastName).headOption.map(_.text)
            val email = (me \\ ConcurProvider.EmailAddress).headOption.map(_.text)
            val fullName = Seq(firstName, lastName).flatten.mkString(" ").trim() match {
              case s: String if !s.isEmpty => Some(s)
              case _ => None
            }
            BasicProfile(id, userId, firstName, lastName, fullName, email, None, authMethod, oAuth2Info = Some(info))
        }
      } recover {
        case e: AuthenticationException => throw e
        case e =>
          logger.error("[securesocial] error retrieving profile information from Concur", e)
          throw new AuthenticationException()
      }
  }

  /**
   * Masks sensitive information so that it doesn't end up in the logs.
   */
  def maskSensitiveInformation(node: Node): Node = node match {
    case <Access_Token>{ ch @ _* }</Access_Token> => <Access_Token>{ ch.map(maskSensitiveInformation) }</Access_Token>
    case <Token>{ contents }</Token> => <Token>*** masked ***</Token>
    case <Refresh_Token>{ contents }</Refresh_Token> => <Refresh_Token>*** masked ***</Refresh_Token>
    case other @ _ => other
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

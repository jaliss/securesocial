package securesocial.core.providers

import org.apache.commons.codec.binary.Base64
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.mvc.Request
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.Future

/**
 * A Reddit provider
 *
 * Relies on Http Basic Authentication to get the access token
 *
 * https://github.com/reddit/reddit/wiki/OAuth2
 */
class RedditProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  val MeUrl = "https://oauth.reddit.com/api/v1/me"
  val Id = "id"
  val Name = "name"

  override val id = RedditProvider.Reddit

  override protected def getAccessToken[A](code: String)(implicit request: Request[A]): Future[OAuth2Info] = {
    val callbackUrl = routesService.authenticationUrl(id)
    val params = Map(
      OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
      OAuth2Constants.Code -> Seq(code),
      OAuth2Constants.RedirectUri -> Seq(callbackUrl)
    ) ++ settings.accessTokenUrlParams.mapValues(Seq(_))

    client.httpService.url(settings.accessTokenUrl).withAuth(settings.clientId, settings.clientSecret, BASIC).post(params)
      .map(buildInfo)
      .recover {
        case e =>
          logger.error("[securesocial] error trying to get an access token for provider %s".format(id), e)
          throw new AuthenticationException()
      }
  }

  val userInfoReader = (
    (__ \ Id).read[String] and
    (__ \ Name).read[String]
  ).tupled

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    client.httpService.url(MeUrl).withHeaders("Authorization" -> s"bearer ${info.accessToken}").get().map { res =>
      userInfoReader.reads(res.json).fold(
        invalid => {
          logger.error("[securesocial] got back error message " + res.body)
          throw new AuthenticationException()
        },
        me => BasicProfile(id, me._1, None, None, Some(me._2), None, None, authMethod, oAuth2Info = Some(info))
      )
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from reddit", e)
        throw new AuthenticationException()
    }
  }
}

object RedditProvider {
  val Reddit = "reddit"
}
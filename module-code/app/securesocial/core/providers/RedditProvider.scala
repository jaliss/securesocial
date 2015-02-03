package securesocial.core.providers

import play.api.libs.ws.WSResponse
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.Future

/**
 * A Reddit provider
 *
 */
class RedditProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  val GetAuthenticatedUser = "https://www.reddit.com/api/v1/me?access_token=%s"
  val AccessToken = "access_token"
  val TokenType = "token_type"
  val Message = "message"
  val Id = "id"
  val Name = "name"
  val AvatarUrl = "avatar_url"
  val Email = "email"

  override val id = RedditProvider.Reddit

  override protected def buildInfo(response: WSResponse): OAuth2Info = {
    val values: Map[String, String] = response.body.split("&").map(_.split("=")).withFilter(_.size == 2)
      .map(r => (r(0), r(1)))(collection.breakOut)
    val accessToken = values.get(OAuth2Constants.AccessToken)
    if (accessToken.isEmpty) {
      logger.error(s"[securesocial] did not get accessToken from $id")
      throw new AuthenticationException()
    }
    OAuth2Info(
      accessToken.get,
      values.get(OAuth2Constants.TokenType),
      values.get(OAuth2Constants.ExpiresIn).map(_.toInt),
      values.get(OAuth2Constants.RefreshToken)
    )
  }

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    client.retrieveProfile(GetAuthenticatedUser.format(info.accessToken)).map { me =>

      logger.info(me.toString())

      (me \ Message).asOpt[String] match {
        case Some(msg) =>
          logger.error(s"[securesocial] error retrieving profile information from Reddit. Message = $msg")
          throw new AuthenticationException()
        case _ =>
          val userId = (me \ Id).as[Int]
          val displayName = (me \ Name).asOpt[String]
          val avatarUrl = (me \ AvatarUrl).asOpt[String]
          val email = (me \ Email).asOpt[String].filter(!_.isEmpty)
          BasicProfile(id, userId.toString, None, None, displayName, email, avatarUrl, authMethod, oAuth2Info = Some(info))
      }
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
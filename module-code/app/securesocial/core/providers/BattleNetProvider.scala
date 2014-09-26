package securesocial.core.providers

import play.api.libs.ws.Response
import securesocial.core.services.{ RoutesService, CacheService }
import securesocial.core._

import scala.concurrent.Future

case class BattleNetOAuth2Info(accessToken: String, accountId: Int, tokenType: Option[String] = None,
  expiresIn: Option[Int] = None, refreshToken: Option[String] = None) extends OAuth2Info

class BattleNetProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  private val Logger = play.api.Logger("securesocial.core.providers.BattleNetProvider")
  override val id = BattleNetProvider.BattleNet

  override protected def buildInfo(response: Response): OAuth2Info = {
    val json = response.json
    logger.debug("[securesocial] got json back [" + json + "]")
    BattleNetOAuth2Info(
      (json \ OAuth2Constants.AccessToken).as[String],
      (json \ BattleNetProvider.AccountIdKey).as[Int],
      (json \ OAuth2Constants.TokenType).asOpt[String],
      (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
      (json \ OAuth2Constants.RefreshToken).asOpt[String]
    )
  }

  override def fillProfile(info: OAuth2Info): Future[BasicProfile] = {

    val profile = info match {
      case x: BattleNetOAuth2Info =>
        BasicProfile(id, x.accountId.toString, None, None, None, None, None, authMethod, None, Some(x))
      case _ =>
        throw new Exception("Invalid oauth 2 profile received, unable to continue")
    }

    Future.successful(profile)
  }

}

object BattleNetProvider {
  val BattleNet = "battlenet"
  val AccountIdKey = "accountId"
}
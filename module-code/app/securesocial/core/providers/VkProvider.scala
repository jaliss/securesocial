package securesocial.core.providers

import play.api.Logger
import play.api.libs.json.JsObject
import securesocial.core._
import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.services.{RoutesService, CacheService, HttpService}


/**
 * A Vk provider
 */
class VkProvider(routesService: RoutesService,
                 httpService: HttpService,
                 cacheService: CacheService,
                 settings: OAuth2Settings = OAuth2Settings.forProvider(VkProvider.Vk))
  extends OAuth2Provider(settings, routesService, httpService, cacheService)
{
  val GetProfilesApi = "https://api.vk.com/method/getProfiles?fields=uid,first_name,last_name,photo&access_token="
  val Response = "response"
  val Id = "uid"
  val FirstName = "first_name"
  val LastName = "last_name"
  val Photo = "photo"
  val Error = "error"
  val ErrorCode = "error_code"
  val ErrorMessage = "error_msg"

  override val id = VkProvider.Vk

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    httpService.url(GetProfilesApi + accessToken).get().map {
      response =>
        val json = response.json
        (json \ Error).asOpt[JsObject] match {
          case Some(error) =>
            val message = (error \ ErrorMessage).as[String]
            val errorCode = (error \ ErrorCode).as[Int]
            Logger.error(
              s"[securesocial] error retrieving profile information from Vk. Error code = $errorCode, message = $message"
            )
            throw new AuthenticationException()
          case _ =>
            val me = (json \ Response).apply(0)
            val userId = (me \ Id).as[Long].toString
            val firstName = (me \ FirstName).asOpt[String]
            val lastName = (me \ LastName).asOpt[String]
            val avatarUrl = (me \ Photo).asOpt[String]
            BasicProfile(id, userId, firstName, lastName, None, None, avatarUrl, authMethod, oAuth2Info = Some(info))
        }
    } recover {
      case e: AuthenticationException => throw e
      case e: Exception =>
        Logger.error("[securesocial] error retrieving profile information from VK", e)
        throw new AuthenticationException()
    }
  }
}

object VkProvider {
  val Vk = "vk"
}

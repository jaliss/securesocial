package securesocial.core.providers

import play.api.libs.ws.WS
import play.api.{ Application, Logger }
import play.api.libs.json.JsObject
import securesocial.core._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, TimeoutException }

/**
 * A Vk provider
 */
class VkProvider(application: Application) extends OAuth2Provider(application) {
  val GetProfilesApi = "https://api.vk.com/method/getProfiles?fields=uid,first_name,last_name,photo&access_token="
  val Response = "response"
  val Id = "uid"
  val FirstName = "first_name"
  val LastName = "last_name"
  val Photo = "photo"
  val Error = "error"
  val ErrorCode = "error_code"
  val ErrorMessage = "error_msg"

  def fillProfile(user: SocialUser) = {
    val accessToken = user.oAuth2Info.get.accessToken

    try {
      val f = WS.url(GetProfilesApi + accessToken).get()
      val response = Await.result(f, 10 seconds)
      val json = response.json
      (json \ Error).asOpt[JsObject] match {
        case Some(error) =>
          val message = (error \ ErrorMessage).as[String]
          val errorCode = (error \ ErrorCode).as[Int]
          Logger.error("[securesocial] error retrieving profile information from Vk. Error code = %s, message = %s"
            .format(errorCode, message))
          throw new AuthenticationException()
        case _ =>
          val me = (json \ Response).apply(0)
          val userId = (me \ Id).as[Long]
          val firstName = (me \ FirstName).as[String]
          val lastName = (me \ LastName).as[String]
          val avatarUrl = (me \ Photo).asOpt[String]
          user.copy(
            id = UserId(userId.toString, id),
            firstName = firstName,
            lastName = lastName,
            fullName = firstName + " " + lastName,
            avatarUrl = avatarUrl,
            email = None)
      }

    } catch {
      case _: TimeoutException =>
        Logger.error("[securesocial] Timeout error retrieving profile information")
        throw new AuthenticationException()
    }

  }

  def id = VkProvider.Vk
}

object VkProvider {
  val Vk = "vk"
}

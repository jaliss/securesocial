/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.controllers

import org.joda.time.DateTime
import securesocial.core._
import play.api.mvc.Action
import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.SignUpEvent
import securesocial.core.AuthenticationResult.Authenticated
import securesocial.core.LoginEvent
import securesocial.core.BasicProfile
import securesocial.core.services.SaveMode

/**
 * A default controller that uses the BasicProfile as the application user type.
 */
class LoginApi(override implicit val env: RuntimeEnvironment[BasicProfile]) extends BaseLoginApi[BasicProfile]

/**
 * This trait provides the means to provide an authentication API that can be used by client side or mobile apps.
 *
 * @tparam U The application user type
 */
trait BaseLoginApi[U] extends SecureSocial[U] {

  import play.api.libs.json._

  case class TokenResponse(token: String, expiresOn: DateTime)

  implicit val jodaDateWrites: Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
    def writes(d: org.joda.time.DateTime): JsValue = JsString(d.toString)
  }
  implicit val HeaderTokenWrites = Json.writes[TokenResponse]
  val logger = play.api.Logger("securesocial.controllers.BaseLoginApi")

  def authenticate(providerId: String, builderId: String) = Action.async { implicit request =>
    import ExecutionContext.Implicits.global
      val result = for (
        builder <- env.authenticatorService.find(builderId) ;
        provider <- env.providers.get(providerId) if provider.isInstanceOf[ApiSupport]
      ) yield {
        provider.asInstanceOf[ApiSupport].authenticateForApi.flatMap {
          case authenticated: Authenticated =>
            val profile = authenticated.profile
            env.userService.find(profile.providerId, profile.userId).flatMap {
              maybeExisting =>
                val mode = if (maybeExisting.isDefined) SaveMode.LoggedIn else SaveMode.SignUp
                env.userService.save(authenticated.profile, mode).flatMap {
                  userForAction =>
                    logger.debug(s"[securesocial] user completed authentication: provider = ${profile.providerId}, userId: ${profile.userId}, mode = $mode")
                    val evt = if (mode == SaveMode.LoggedIn) new LoginEvent(userForAction) else new SignUpEvent(userForAction)
                    // we're not using a session here .... review this.
                    Events.fire(evt)
                    builder.fromUser(userForAction).map { authenticator =>
                      val token = TokenResponse(authenticator.id, authenticator.expirationDate)
                      Ok(Json.toJson(token))
                    }
                }
            }
          case failed: AuthenticationResult.Failed =>
            Future.successful(BadRequest(Json.toJson(Map("error" -> failed.error))).as("application/json"))
          case other  =>
            // todo: review this status
            logger.error(s"[securesocial] unexpected result from authenticateForApi: $other")
            Future.successful(InternalServerError(Json.toJson(Map("error" -> "unexpected internal error"))).as("application/json"))
        }
      }
      result.getOrElse(Future.successful(NotFound.as("application/json")))
  }

  def logout = Action.async { implicit request =>
    import securesocial.core.utils._
    import ExecutionContext.Implicits.global
    env.authenticatorService.fromRequest(request).flatMap {
      case Some(authenticator) =>  Ok("").discardingAuthenticator(authenticator)
      case None => Future.successful(Ok(""))
    }
  }
}


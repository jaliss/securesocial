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

import play.api.data.Form
import play.api.data.Forms._
import securesocial.core._
import play.api.mvc.{PlainResult, Results, Result, Request}
import utils.PasswordHasher
import play.api.{Play, Application}
import Play.current
import com.typesafe.plugin._

/**

 */

class UsernamePasswordProvider(application: Application) extends IdentityProvider(application) {

  def providerId = UsernamePasswordProvider.UsernamePassword

  def authMethod = AuthenticationMethod.UserPassword

  val InvalidCredentials = "securesocial.login.invalidCredentials"

  def doAuth[A]()(implicit request: Request[A]): Either[Result, SocialUser] = {
    val form = UsernamePasswordProvider.loginForm.bindFromRequest()
    form.fold(
      errors => Left(badRequest(errors)),
      credentials => {
        val userId = UserId(credentials._1, providerId)
        UserService.find(userId) match {
          case Some(user) if user.passwordInfo.isDefined && use[PasswordHasher].matches(user.passwordInfo.get, credentials._2) =>
            Right(user)
          case _ => Left(badRequest(UsernamePasswordProvider.loginForm, Some(InvalidCredentials)))
        }
      }
    )
  }

  private def badRequest(f: Form[(String,String)], msg: Option[String] = None): PlainResult = {
    Results.BadRequest(securesocial.views.html.login(ProviderRegistry.all().values, f, msg))
  }

  def fillProfile(user: SocialUser) = {
    // nothing to do for this provider, the user should already have everything because it
    // was loaded from the backing store
    user
  }
}

object UsernamePasswordProvider {
  val UsernamePassword = "userpass"

  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )
}
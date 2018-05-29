/**
 * Copyright 2013-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package service

import securesocial.core._
import play.api.mvc.{ RequestHeader, Session }
import play.api.Logger
import play.api.i18n.{ I18nSupport, MessagesApi }

/**
 * A sample event listener
 */
class MyEventListener()(implicit val messagesApi: MessagesApi) extends EventListener with I18nSupport {

  def onEvent[U](event: Event[U], request: RequestHeader, session: Session): Option[Session] = {
    val eventName = event match {
      case LoginEvent(u) => "login"
      case LogoutEvent(u) => "logout"
      case SignUpEvent(u) => "signup"
      case PasswordResetEvent(u) => "password reset"
      case PasswordChangeEvent(u) => "password change"

    }

    event match {
      case Event(u: DemoUser) => Logger.info("traced %s event for user %s".format(eventName, u.main.userId))
    }

    // retrieving the current language
    Logger.info("current language is %s".format(request.lang))

    // Not changing the session so just return None
    // if you wanted to change the session then you'd do something like
    // Some(session + ("your_key" -> "your_value"))
    None
  }

}

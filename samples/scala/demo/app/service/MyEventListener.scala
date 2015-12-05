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
import play.api.mvc.{ Session, RequestHeader }
import play.api.Logger

class MyEventListener extends EventListener {
  def onEvent[U](event: Event[U], request: RequestHeader, session: Session): Option[Session] = {
    def log(user: U, action: String) = user match {
      case u: DemoUser => Logger.info(s"${u.main.fullName} (${u.main.userId}) $action")
    }

    event match {
      case LoginEvent(user) =>
        log(user, "logged in")

      case LogoutEvent(user) =>
        log(user, "logged out")

      case SignUpEvent(user) =>
        log(user, "signed up")

      case PasswordResetEvent(user) =>
        log(user, "reset their password")

      case PasswordChangeEvent(user) =>
        log(user, "changed their password")
    }

    Logger.info(s"current language is ${request2lang(request)}")

    // Not changing the session so just return None
    // if you wanted to change the session then you'd do something like
    // Some(session + ("your_key" -> "your_value"))
    None
  }
}

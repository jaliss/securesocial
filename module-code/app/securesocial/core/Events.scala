/**
 * Copyright 2013 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core

import play.api.mvc.{Session, RequestHeader}
import play.api.{Logger, Plugin}

/**
 * A trait to model SecureSocial events
 */
sealed trait Event { def user: Identity }

/**
 * The event fired when a users logs in
 * @param user
 */
case class LoginEvent(user: Identity) extends Event

/**
 * The event fired when a user logs out
 * @param user
 */
case class LogoutEvent(user: Identity) extends Event

/**
 * The event fired when a user sings up with the Username and Password Provider
 * @param user
 */
case class SignUpEvent(user: Identity) extends Event

/**
 * The event fired when a user changes his password
 * @param user
 */
case class PasswordChangeEvent(user: Identity) extends Event

/**
 * The event fired when a user completes a password reset
 * @param user
 */
case class PasswordResetEvent(user: Identity) extends Event

/**
 * The event listener interface
 */
abstract class EventListener extends Plugin with Registrable {
  override def onStart() {
    Logger.info("[securesocial] loaded event listener %s".format(id))
    Registry.eventListeners.register(this)
  }


  override def onStop() {
    Logger.info("[securesocial] unloaded event listener %s".format(id))
    Registry.eventListeners.unRegister(id)
  }
  /**
   * The method that gets called when an event occurs.
   *
   * @param event the event type
   * @param request the current request
   * @param session the current session (if you need to manipulate it don't use the one in request.session)
   * @return can return an optional Session object.
   */
  def onEvent(event: Event, request: RequestHeader, session: Session): Option[Session]
}

/**
 * Helper object to fire events
 */
object Events {

  def doFire(list: List[EventListener], event: Event,
             request: RequestHeader, session: Session): Session =
  {
    if ( list.isEmpty ) {
      session
    } else {
      val newSession = list.head.onEvent(event, request, session)
      doFire(list.tail, event, request, newSession.getOrElse(session))
    }
  }

  def fire(event: Event)(implicit request: RequestHeader): Option[Session] = {
    val listeners = Registry.eventListeners.all().toList.map(_._2)
    val result = doFire(listeners, event, request, request.session)
    if ( result == request.session ) None else Some(result)
  }
}

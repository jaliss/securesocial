package service

import securesocial.core.{LogoutEvent, LoginEvent, Event, EventListener}
import play.api.mvc.{Session, RequestHeader}
import play.api.{Application, Logger}

/**
 * A sample event listener
 */
class MyEventListener(app: Application) extends EventListener {
  override def id: String = "my_event_listener"

  def onEvent(event: Event, request: RequestHeader, session: Session): Option[Session] = {
    event match {
      case e: LoginEvent => Logger.info("traced login event for user %s".format(e.user.fullName))
      case e: LogoutEvent => Logger.info("traced logout event for user %s".format(e.user.fullName))
    }
    // Not changing the session so just return None
    // if you wanted to change the session then you'd do something like
    // Some(session + ("your_key" -> "your_value"))
    None
  }
}
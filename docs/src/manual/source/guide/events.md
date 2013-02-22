---
file: events
---
# Events

SecureSocial fires the following events that can be listened by your application:

- Login
- Logout
- Sign up (for the Username and Password provider)
- Password Change
- Password Reset

## Event Listener

To start tracking events create a new class that extends `EventListener` and implement the `onEvent` method. 

	:::scala
	class MyEventListener(app: Application) extends EventListener {
		override def id: String = "my_event_listener"

	  	def onEvent(event: Event, request: RequestHeader, session: Session): Option[Session] = {
		    val eventName = event match {
		      case e: LoginEvent => "login"
		      case e: LogoutEvent => "logout"
		      case e: SignUpEvent => "signup"
		      case e: PasswordResetEvent => "password reset"
		      case e: PasswordChangeEvent => "password change"
		    }
		    Logger.info("traced %s event for user %s".format(eventName, event.user.fullName))
		    None
  		}
	}

You modify the session after handling the event by using the `session` parameter and returning a new instance with the required changes.  Returning `None` means that nothing needs to be changed as in the example above.  

If you wanted to change the session you would replace the `None` in the sample above with something like:

	:::scala
	Some(session + ("my_key" -> "my_value"))	    
		    
### Java

The Event API is currently available in Scala only.  
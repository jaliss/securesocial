---
file: authorization
---
# Adding Authorization

SecureSocial provides a way to add authorization logic to your controller actions.  This is done by implementing an `Authorization` object that is passed to the `SecuredAction` as a parameter.

After checking if a user is authenticated the `Authorization` instance is used to verify if the execution should be allowed or not.

## Scala

For Scala, you need to implement the `Authorization` trait.

	:::scala
	trait Authorization {		
		def isAuthorized(user: Identity): Boolean
	}

This is a sample implementation that only grants acccess to users that logged in usign a given provider:

	:::scala
	case class WithProvider(provider: String) extends Authorization {
	  def isAuthorized(user: Identity) = {
	    user.id.providerId == provider
	  }
	}

Here's how you would use it:

	:::scala
	def myAction = SecuredAction(WithProvider("twitter")) { implicit request =>
		// do something here
	}

## Java

For Java, you need to implement the `Authorization` interface.

	:::java
	public interface Authorization {
 	   boolean isAuthorized(Identity user, String[] params);
	}

This is an equivalent implementation to the Scala sample:

	:::java
	public class WithProvider implements Authorization {
	    public boolean isAuthorized(Identity user, String params[]) {
	        return user.id().providerId().equals(params[0]);
	    }
	}

Here's how you would use it:

	:::java
	@SecureSocial.SecuredAction( authorization = WithProvider.class, params = {"twitter"})
    public static Result myAction() {
        // do something here
    }

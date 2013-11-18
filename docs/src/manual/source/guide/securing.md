---
file: securing
---
# Securing your Actions

SecureSocial provides a replacement for Play's buit in Action class named `SecuredAction`. This action intercepts requests and checks if there is an authenticated user.  If there is one the execution continues and your code is invoked, if not the request is redirected to a login page.  For ajax calls you can make SecureSocial return an unauthorized error instead of a redirect.

There is also a `UserAwareAction` that can be used in actions that need to know if there is a current user but can be executed even if there isn't one.

Here's a sample usage in Scala:
	
	:::scala
	// 1) add the SecureSocial trait to your controller
	object Application extends Controller with securesocial.core.SecureSocial {	  
	  // 2) change Play's Action with SecuredAction
	  def index = SecuredAction() { implicit request =>
	    Ok(views.html.index(request.user))
	  }	 

	  def page = UserAwareAction { implicit request =>
    	val userName = request.user match {
	      	case Some(user) => user.fullName
	      	case _ => "guest"
    	}
   		 Ok("Hello %s".format(userName))
	  }

	  // you don't want to redirect to the login page for ajax calls so
	  // adding a ajaxCall = true will make SecureSocial return a forbidden error
	  // instead.
	  def ajaxCall = SecuredAction(ajaxCall = true) { implicit request =>
	  	// return some json
	  }   
	}
	
Note that you get access to the current user in the request using `request.user`.  This points to an instance of the `Identity` trait for `SecuredAction` and to an `Option[Identity]` for `UserAwareAction`.

In Java, you use annotations:

	:::java
	public class Application extends Controller {
	    @SecureSocial.SecuredAction
	    public static Result index() {
	        Identity user = (Identity) ctx().args.get(SecureSocial.USER_KEY);
	        return ok(index.render(user));
	    }

	    @SecureSocial.UserAwareAction
	    public static Result userAware() {
	        Identity user = (Identity) ctx().args.get(SecureSocial.USER_KEY);
	        final String userName = user != null ? user.fullName() : "guest";
	        return ok("Hello " + userName);
	    }

	    @SecureSocial.SecuredAction(ajaxCall = true)
	    public static Result ajaxCall() {
	        // return some json
	    }
	}

The current user (always present for `SecuredAction`) is available in the request context arguments.

## Identity

The `Identity` is defined using a trait (traits are similar to Java interfaces). The following attributes are defined in this trait:

- `identityId`: a `IdentityId` object that stores the user `userId` within the provider used to authenticate the user (eg: the twitter id) and the `providerId`. 
- `firstName`, `lastName` and `fullName`: The user's names.
- `email`: The user email address (available if the external service provides it). Eg: Twitter does not expose email addresses.
- `avatarUrl`: The url that points to the user image in the authenticating service.

Depending on how the user authenticated the following will be available too:

- `oAuth1Info`: an instance of `OAuth1Info` containing the `token` and `secret`.
- `oAuth2Info`: an instance of `OAuth2Info` containing the `accessToken` and optionals `tokenType`, `expiresIn` and `refreshToken` values.
- `passwordInfo`: an instance of `PasswordInfo` containing the hashed `password` and optinally the `salt` used to hash it.

For OAuth based logins the info fields should have all the information needed to invoke APIs on those external services.

### A Note for Java developers

As mentioned above, `Identity` is defined using Scala.  From Java, you see this trait as an interface and can access the attributes as if they were methods.

For example:

	:::java	
	 // accessing the first name 
	 String msg = "Hello " + user.firstName()  
	 

#### Option objects

Some attributes in `Identity` such as `email` or `oAuth1Info` are defined using an `Option`.  In Scala, it is very common to use an `Option` to model an optional value instead of leaving it null as in Java.

To access these attributes you need to first check if the `Option` has a value in it (the equivalent to checking if it's null in Java).  

For example:

	:::java
	String email = user.email().isDefined() ? user.email().get() : "Not available"

	if ( user.oAuth1Info().isDefined() ) {
		// there is a value 
		OAuth1Info info = user.oAuth1Info().get()
	}

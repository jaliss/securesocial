---
file: user-service
---
# UserService

SecureSocial relies on an implementation of `UserService` to handle all the operations related to saving/finding users. Using this delegation model you are not forced to use a particular model object or a persistence mechanism but rather provide a service that translates back and forth between your models and what SecureSocial understands. 

Besides users, this service is also in charge of persisting the tokens that are used in signup and reset password requests. Some of these methods are only required if you are going to use the `UsernamePasswordProvider`. If you do not plan to use it just provide an empty implementation for them.  Check the documentation in the source code to know which ones are optional.

## SocialUser

`SocialUser` is an object that implements the `Identity` trait. An instance of this class is created by SecureSocial to gather information about the user when he signs up and/or signs in. 

## Identity and SocialUser

As you will see, some methods in `UserService` receive or return `Identity` instances.  When your implementation receives an `Identity` (eg: in the `save` method) the class instantiated is actually a `SocialUser`.  

When you return an `Identity` from the `find` method you can return an instance of a `SocialUser` or your own object that implements `Identity`. Since the instance that gets returned by `find` is passed back to your controller actions this allows you to use your own class when handling requests. This is useful because you can easily convert an `Identity` to your own model by using pattern matching in Scala or casting in Java.

As an example, if you had a `User` class that implemented `Identity` you'd convert it as follows:

	:::scala
	def myAction = SecuredAction { implicit request =>
		request.user match { 
			case user: User => {
				// handle request
			}
		    case _ => // did not get a User instance, log error throw exception 
		}
	}

In Java, you would do something like:

	:::java
	public static Result myAction() {
        User user = (User) ctx().args.get(SecureSocial.USER_KEY);
        return ok(index.render(user));
    }

## How to implement

For Scala you need to extend the `UserServicePlugin`. For example:

	:::scala
	class MyUserService(application: Application) extends UserServicePlugin(application) {
	  /**
	   * Finds a user that maches the specified id
	   *
	   * @param id the user id
	   * @return an optional user
	   */
	  def find(id: IdentityId):Option[Identity] = {
	  	// implement me
	  }

	  /**
	   * Finds a user by email and provider id.
	   *
	   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	   * implementation.
	   *
	   * @param email - the user email
	   * @param providerId - the provider id
	   * @return
	   */
	  def findByEmailAndProvider(email: String, providerId: String):Option[Identity] =
	  {
	  	// implement me
	  }

	  /**
	   * Saves the user.  This method gets called when a user logs in.
	   * This is your chance to save the user information in your backing store.
	   * @param user
	   */
	  def save(user: Identity) {
	  	// implement me
	  }

	  /**
	   * Saves a token.  This is needed for users that
	   * are creating an account in the system instead of using one in a 3rd party system.
	   *
	   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	   * implementation
	   *
	   * @param token The token to save
	   * @return A string with a uuid that will be embedded in the welcome email.
	   */
	  def save(token: Token) = {
	  	// implement me
	  }


	  /**
	   * Finds a token
	   *
	   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	   * implementation
	   *
	   * @param token the token id
	   * @return
	   */
	  def findToken(token: String): Option[Token] = {
	  	// implement me
	  }

	  /**
	   * Deletes a token
	   *
	   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	   * implementation
	   *
	   * @param uuid the token id
	   */
	  def deleteToken(uuid: String) {
	  	// implement me
	  }

	  /**
	   * Deletes all expired tokens
	   *
	   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	   * implementation
	   *
	   */
	  def deleteExpiredTokens() {
	  	// implement me
	  }
	}


For Java, you need to extend the `BaseUserService` class.

	:::java
	public class MyUserService extends BaseUserService {
    
	    public MyUserService(Application application) {
	        super(application);
	    }

	    /**
	     * Saves the user.  This method gets called when a user logs in.
	     * This is your chance to save the user information in your backing store.
	     * @param user
	     */
	    @Override
	    public void doSave(Identity user) {
	        // implement me
	    }

		/**
	     * Finds an Identity in the backing store.	     
	     * @return an Identity instance or null if no user matches the specified id
	     */
	    @Override
	    public Identity doFind(IdentityId id) {
	        // implement me
	    }

	    /**
	     * Finds an identity by email and provider id.
	     *
	     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	     * implementation.
	     *
	     * @param email - the user email
	     * @param providerId - the provider id
	     * @return an Identity instance or null if no user matches the specified id
	     */
	    @Override
	    public Identity doFindByEmailAndProvider(String email, String providerId) {
	        // implement me
	    }

	    /**
	     * Saves a token
	     */
	    @Override
	    public void doSave(Token token) {
	        // implement me
	    }

		/**
		 * Finds a token by id
		 *
		 * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	     * implementation
	     *
		 * @return a Token instance or null if no token matches the id
		 */
	    @Override
	    public Token doFindToken(String tokenId) {
	        // implement me
	    }

	    
	 	/**
	     * Deletes a token
	     *
	     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	     * implementation
	     *
	     * @param uuid the token id
	     */
	    @Override
	    public void doDeleteToken(String uuid) {
	        // implement me
	    }

	    /**
	     * Deletes all expired tokens
	     *
	     * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	     * implementation
	     *
	     */
	    @Override
	    public void doDeleteExpiredTokens() {
	        // implement me
	    }
	}

*Note: the Scala and Java samples come with a memory based implementation that can be used as a starting point for your own implementation.*

# Important

Note that the `Token` class is implemented in Scala and Java.  Make sure you import the one that matches the language you are using in your `UserService` implementation.

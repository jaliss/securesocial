---
file: user-service
---
# UserService

SecureSocial relies on an implementation of `UserService` to save/find users. Using this delegation model you are not forced to use a particular model object but rather provide a service that translates back and forth between your models and what SecureSocial understands. 

Besides users, this service is also in charge of persisting the tokens that are used in signup and reset password requests. Some of these methods are only required if you are going to use the `UsernamePasswordProvider`. If you do not plan to use it just provide an empty implementation for them.  Check the documentation in the source code to know which ones are optional.

For Scala you need to extend the `UserServicePlugin`. For example:

	:::scala
	class MyUserService(application: Application) extends UserServicePlugin(application) {
	  /**
	   * Finds a SocialUser that maches the specified id
	   *
	   * @param id the user id
	   * @return an optional user
	   */
	  def find(id: UserId):Option[SocialUser] = {
	  	// implement me
	  }

	  /**
	   * Finds a Social user by email and provider id.
	   *
	   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
	   * implementation.
	   *
	   * @param email - the user email
	   * @param providerId - the provider id
	   * @return
	   */
	  def findByEmailAndProvider(email: String, providerId: String):Option[SocialUser] =
	  {
	  	// implement me
	  }

	  /**
	   * Saves the user.  This method gets called when a user logs in.
	   * This is your chance to save the user information in your backing store.
	   * @param user
	   */
	  def save(user: SocialUser) {
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

	    @Override
	    public void doSave(SocialUser user) {
	        // implement me
	    }

	    @Override
	    public void doSave(Token token) {
	        // implement me
	    }

	    @Override
	    public SocialUser findByEmailAndProvider(UserId userId) {
	        // implement me
	    }

	    @Override
	    public Token doFindToken(String tokenId) {
	        // implement me
	    }

	    @Override
	    public SocialUser doFindByEmail(String email, String providerId) {
	        // implement me
	    }

	    @Override
	    public void doDeleteToken(String uuid) {
	        // implement me
	    }

	    @Override
	    public void doDeleteExpiredTokens() {
	        // implement me
	    }
	}

*Note: the Scala and Java samples come with a memory based implementation that can be used as a starting point for your own implementation.*

# Important

You'll notice that some class names in the Scala and Java samples are the same but they are actually different classes residing in separate packages.  Check the `securesocial.core.java` package for the Java ones.

SecureSocial provides the same objects in Java and Scala implementations and converts automatically between them behind the scenes so you only deal with the language you need.

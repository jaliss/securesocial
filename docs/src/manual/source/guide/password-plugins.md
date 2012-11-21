---
file: password-plugins
---
# Password Plugins

When users sign up and enter a password it is required to enforce some strength on them to disallow really week ones.  Storing passwords in clear text is not recommended either.  To address these two things SecureSocial uses the following plugins:

- `PasswordValidador`: Used when users submit the registration form to validate the password.

- `PasswordHasher`: Used to hash the password entered by the user prior to saving it.

The modules comes with default implementations for each:

- `DefaultPasswordValidator`: A validator that restricts passwords to a minimum length. By default the length is 8 but can be changed by setting the `minimumPasswordLength` in the properties file.

- `BCryptPasswordHasher`: A password hasher based on the bcrypt algorithm. 

This will be good enough for many cases, but if you need to change the way password length/strength is enforced and/or how they are hashed you can write your own plugins and register them in the `play.plugins` file instead of the ones provided by SecureSocial.

## PasswordValidator

### Scala

For Scala, you need to implement the `PasswordValidator` trait:

	:::scala
	trait PasswordValidator extends Plugin {
	  	def isValid(password: String): Boolean
	  	def errorMessage: String
    }

- `isValid`: Must return true or false depending on whether the supplied passwors is good enough for the validator.

- `errorMessage`: An error message that will be shown on the sign up page if the password is invalid.

You will also need to add a constructor that receives an `Application` instance.

### Java

For Java, create a subclass of `BasePasswordValidator` and implement the `isValid` and `errorMessage` methods as described above and also add a public constructor that receives an `Application` instance.

    :::java
    public boolean isValid(String password)
    public String errorMessage() 	
    
## PasswordHasher

### Scala

For Scala, you need to implement the `PasswordHasher` trait:

	:::scala
	trait PasswordHasher extends Plugin {
  		def hash(plainPassword: String): PasswordInfo
		def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean
	}

- `hash`: this method hashes the password and returns a `PasswordInfo` containing the hashed password and optionally the salt used to hash it.

- `matches`: checks if the `suppliedPassword` matches the hashed one in `passwordInfo`.

### Java

For Java, extend the `BasePasswordHasher` class and implement the `doHash` and `doMatch` mathods:

	:::java	
   	PasswordInfo doHash(String plainPassword);    
    boolean doMatch(PasswordInfo passwordInfo, String suppliedPassword);


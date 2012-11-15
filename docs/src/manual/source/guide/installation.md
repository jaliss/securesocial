---
file: installation
---
# Installation

### Adding the dependency

SecureSocial is available as a downloadable dependency.  There is repository hosted on the project site with stable releases and snapshots.  To include the module in your project add the folling dependency to your `Build.scala` file:

	:::scala
	"securesocial" % "securesocial_2.9.1" % "2.0.5"

Next, add a resolver so sbt can locate the dependency. There are two locations:

- **Releases**: http://securesocial.ws/repository/releases.  
- **Snapshots**: http://securesocial.ws/repository/snapshots.

Choose either depending on whether you'd like to use a stable release or test the latest changes. This is a sample `Build.scala` file using a stable release:


	:::scala
	object ApplicationBuild extends Build {
	    val appName         = "myapp"
	    val appVersion      = "1.0-SNAPSHOT"

	    val appDependencies = Seq(
	        "securesocial" % "securesocial_2.9.1" % "2.0.5"
	    )
	    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
	      resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/releases/"))(Resolver.ivyStylePatterns)
	    )
	}

*Make sure to set the `mainLang` to the main language of your application. Valid options are `JAVA` or `SCALA`.*

If you'd like to use the master snapshot change it to:

	:::scala	
    val appDependencies = Seq(
        "securesocial" % "securesocial_2.9.1" % "master"
    )
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += Resolver.url("SecureSocial Repository", url("http://securesocial.ws/repository/snapshots/"))(Resolver.ivyStylePatterns)
    )

### Adding routes

Add the routes for SecureSocial into your app's `routes` file:

	:::bash
	# Login page
	GET     /login                      securesocial.controllers.LoginPage.login
	GET     /logout                     securesocial.controllers.LoginPage.logout

	# User Registration
	GET     /signup                     securesocial.controllers.Registration.startSignUp
	POST    /signup                     securesocial.controllers.Registration.handleStartSignUp
	GET     /signup/:token              securesocial.controllers.Registration.signUp(token)
	POST    /signup/:token              securesocial.controllers.Registration.handleSignUp(token)
	GET     /reset                      securesocial.controllers.Registration.startResetPassword
	POST    /reset                      securesocial.controllers.Registration.handleStartResetPassword
	GET     /reset/:token               securesocial.controllers.Registration.resetPassword(token)
	POST    /reset/:token               securesocial.controllers.Registration.handleResetPassword(token)

	# Providers entry points
	GET     /authenticate/:provider     securesocial.controllers.ProviderController.authenticate(provider)
	POST    /authenticate/:provider     securesocial.controllers.ProviderController.authenticateByPost(provider)

### Choosing components

SecureSocial is designed in a modular architecture using plugins. This means you can easily enable/disable them to include only what you need. Plugins are defined in the `play.plugins` file under the `conf` directory. If you don't have that file yet create one and add:
	
	:::bash
	1500:com.typesafe.plugin.CommonsMailerPlugin
	9997:securesocial.controllers.DefaultTemplatesPlugin
	9998:your.user.Service.Implementation <-- Important: You need to change this
	9999:securesocial.core.providers.utils.BCryptPasswordHasher
	10000:securesocial.core.providers.TwitterProvider
	10001:securesocial.core.providers.FacebookProvider
	10002:securesocial.core.providers.GoogleProvider
	10003:securesocial.core.providers.LinkedInProvider
	10004:securesocial.core.providers.UsernamePasswordProvider
	10005:securesocial.core.providers.GitHubProvider
	
*If you don't plan to have a username and password login you can remove `UsernamePasswordProvider`, `CommonsMailerPlugin` and `BCryptPasswordHasher`.*

## Important

There is one plugin you need to write for your apps, and that is the `UserService`.  The line that starts with 9998 above is just a place holder where you need to enter the class of your implentation. To start, you can copy the `InMemoryUserService` provided in the samples (Scala or Java) and then change it.

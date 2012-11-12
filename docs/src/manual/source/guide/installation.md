---
file: installation
---
# Installation

First grab the sources from GitHub.
	
	:::bash
    $ git clone https://github.com/jaliss/securesocial.git

Within your Play app create a directory called `modules/securesocial` and copy the contents of the `module-code` directory to it.  If you are using a unix based system you can create a symbolic link. For example:

	:::bash
    $ cd myapp    
	$ mkdir securesocial
	$ cd securesocial
	$ ln -s ~/projects/securesocial/module-code/ ./securesocial

### WHAT!?!?	

I know, I know. Copying the contents of the module into your project is not nice. There is, I think, a good reason for it.  There's a bug in Play that makes routes in submodules override the ones in the main app if they are included as a downloadable dependency in the `Build.scala` file.  There is going to be a fix in Play 2.1, so in the mean time this is the way to work around the problem. Rest assured that once that is fixed this process is going to be better :) 

Ok, moving on. Change your `Build.scala` file to include the module by adding:

	:::scala
	 val ssDependencies = Seq(
	      "com.typesafe" %% "play-plugins-util" % "2.0.3",
	      "com.typesafe" %% "play-plugins-mailer" % "2.0.4",
	      "org.mindrot" % "jbcrypt" % "0.3m"
	    )

	    val secureSocial = PlayProject(
	        "securesocial", appVersion, ssDependencies, mainLang = SCALA, path = file("modules/securesocial")
	    ).settings(
	      resolvers ++= Seq(
	        "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
	        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
	      )
	    )

Then, add the module as a depedency of your app using `depensOn` and `aggregate` as follows:

	:::scala
	val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // 
      // your projects settings 
      // 
    ).dependsOn(secureSocial).aggregate(secureSocial) /// this adds securesocial

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

SecureSocial is designed in a modular architecture using Play plugins. You need to configure your `play.plugins` file to include the components you want to use in your app. If you don't have that file yet create one and add:
	
	:::bash
	1500:com.typesafe.plugin.CommonsMailerPlugin
	9997:securesocial.controllers.DefaultTemplatesPlugin
	9998:service.InMemoryUserService
	9999:securesocial.core.providers.utils.BCryptPasswordHasher
	10000:securesocial.core.providers.TwitterProvider
	10001:securesocial.core.providers.FacebookProvider
	10002:securesocial.core.providers.GoogleProvider
	10003:securesocial.core.providers.LinkedInProvider
	10004:securesocial.core.providers.UsernamePasswordProvider
	10005:securesocial.core.providers.GitHubProvider
	
 If you don't want to have a username and password login you can remove `UsernamePasswordProvider`, `CommonsMailerPlugin` and `BCryptPasswordHasher`.

## Important

There is one plugin you need to write for your apps, and that is the `UserService`.  To start, you can copy the `InMemoryUserService` provided in the samples (Scala or Java) and then change it.

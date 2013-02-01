---
file: views-customization
---

# Views and Email Customization 

## Customizing SecureSocial Static Files

SecureSocial allows you to modify the paths used to access some of its static resources, like `JQuery` or `Bootstrap`, so you can load your own modified files. Using this feature you can ensure that both SecureSocial and your application run the same versions of the libraries or apply minor style changes without using `TemplatesPlugin`.

To modify a static file you just need to add a value in the configuration file indicating the path of the file to be used. SecureSocial will detect that and use your file. If a configuration key is missing then the default file included by SecureSocial will be used.

You can use the following configuration keys:

1. `securesocial.bootstrapCssPath`: the path to your own version of Bootstrap's CSS file. 
2. `securesocial.faviconPath`: the path to your own Favicon, to be displayed while in SecureSocial pages
3. `securesocial.jqueryPath`: the path to your own version of JQuery

There is an additional configuration entry: `securesocial.customCssPath`. If you provide a path to a CSS file, this file will be injected into all SecureSocial pages. This allows you to apply some customization to SecureSocial components without having to create a full set of templates. By default no file is injected.


## Customizing SecureSocial Templates

SecureSocial uses a `TemplatesPlugin` implementation to render the login, signup and password reset pages and generate the email content that is sent by the `UsernamePasswordProvider`. 

The module comes with a default implementation named `DefaultTemplatesPlugin` that you can replace with your own to change the generated html.

To create custom pages:

1. Create a new directory under `views` to place the custom templates for SecureSocial.
2. Create a new plugin that implements the `TemplatesPlugin` trait and renders those templates.
3. Edit the `play.plugins` file and replace `DefaultPluginsTemplate` with your own class.

For example, if the custom templates were placed in the `views/custom` directory your plugin would look like:

	:::scala
	class MyViews(application: Application) extends TemplatesPlugin 
	{
	 /**
	   * Returns the html for the login page
	   * @param request
	   * @tparam A
	   * @return
	   */
	  override def getLoginPage[A](implicit request: Request[A], form: Form[(String, String)],
	                               msg: Option[String] = None): Html =
	  {
	    views.custom.html.login(form, msg)
	  }

	  /**
	   * Returns the html for the signup page
	   *
	   * @param request
	   * @tparam A
	   * @return
	   */
	  override def getSignUpPage[A](implicit request: Request[A], form: Form[RegistrationInfo], token: String): Html = {
	    views.custom.html.Registration.signUp(form, token)
	  }

	  /**
	   * Returns the html for the start signup page
	   *
	   * @param request
	   * @tparam A
	   * @return
	   */
	  override def getStartSignUpPage[A](implicit request: Request[A], form: Form[String]): Html = {
	    views.custom.html.Registration.startSignUp(form)
	  }

	  /**
	   * Returns the html for the reset password page
	   *
	   * @param request
	   * @tparam A
	   * @return
	   */
	  override def getStartResetPasswordPage[A](implicit request: Request[A], form: Form[String]): Html = {
	    views.custom.html.Registration.startResetPassword(form)
	  }

	  /**
	   * Returns the html for the start reset page
	   *
	   * @param request
	   * @tparam A
	   * @return
	   */
	  def getResetPasswordPage[A](implicit request: Request[A], form: Form[(String, String)], token: String): Html = {
	    views.custom.html.Registration.resetPasswordPage(form, token)
	  }

	   /**
	   * Returns the html for the change password page
	   *
	   * @param request
	   * @param form
	   * @tparam A
	   * @return
	   */
	  def getPasswordChangePage[A](implicit request: SecuredRequest[A], form: Form[ChangeInfo]): Html = {
		views.custom.html.passwordChange(form)	  	
	  }


	  /**
	   * Returns the email sent when a user starts the sign up process
	   *
	   * @param token the token used to identify the request
	   * @param request the current http request
	   * @return a String with the html code for the email
	   */
	  def getSignUpEmail(token: String)(implicit request: RequestHeader): String = {
	    views.custom.html.mails.signUpEmail(token).body
	  }

	  /**
	   * Returns the email sent when the user is already registered
	   *
	   * @param user the user
	   * @param request the current request
	   * @return a String with the html code for the email
	   */
	  def getAlreadyRegisteredEmail(user: SocialUser)(implicit request: RequestHeader): String = {
	    views.custom.html.mails.alreadyRegisteredEmail(user).body
	  }

	  /**
	   * Returns the welcome email sent when the user finished the sign up process
	   *
	   * @param user the user
	   * @param request the current request
	   * @return a String with the html code for the email
	   */
	  def getWelcomeEmail(user: SocialUser)(implicit request: RequestHeader): String = {
	    views.custom.html.mails.welcomeEmail(user).body
	  }

	  /**
	   * Returns the email sent when a user tries to reset the password but there is no account for
	   * that email address in the system
	   *
	   * @param request the current request
	   * @return a String with the html code for the email
	   */
	  def getUnknownEmailNotice()(implicit request: RequestHeader): String = {
	    views.custom.html.mails.unknownEmailNotice(request).body
	  }

	  /**
	   * Returns the email sent to the user to reset the password
	   *
	   * @param user the user
	   * @param token the token used to identify the request
	   * @param request the current http request
	   * @return a String with the html code for the email
	   */
	  def getSendPasswordResetEmail(user: SocialUser, token: String)(implicit request: RequestHeader): String = {
	    views.custom.html.mails.passwordResetEmail(user, token).body
	  }

	  /**
	   * Returns the email sent as a confirmation of a password change
	   *
	   * @param user the user
	   * @param request the current http request
	   * @return a String with the html code for the email
	   */
	  def getPasswordChangedNoticeEmail(user: SocialUser)(implicit request: RequestHeader): String = {
	    views.custom.html.mails.passwordChangedNotice(user).body
	  }
	}


*Note: This plugin is only exposed in the Scala language because the implementation is really trivial and usually will be just changing one line to invoke your own view instead of the provided ones.  It makes no sense in this case to provide a Java version.*

## Important

The templates that come with SecureSocial build URLs using http or https depending on how the `ssl` property was set in the `securesocial.conf` file.  For example, in the sign up page you can find the url used by the form is built as:

	:::html
	<form action="@securesocial.core.providers.utils.RoutesHelper.handleSignUp(token).absoluteURL(IdentityProvider.sslEnabled)"

**Make sure you do it the same way in your templates.**




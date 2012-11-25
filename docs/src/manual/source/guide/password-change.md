---
file: password-change
---
# Password Change Page

If you use the `UsernamePasswordProvider` you can add a change password page to your app too.  

### Scala

Assuming your template received a `user` parameter with an instance of `SocialUser` you would add a link to the Password Change page as follows:

	:::html
	@user.passwordInfo.map { info =>
        <a class="btn" href="@securesocial.core.providers.utils.RoutesHelper.changePasswordPage.absoluteURL(IdentityProvider.sslEnabled)">
        	Change Password
        </a>
    }

Make sure to add the following import statement in your template:

	:::scala
	@import securesocial.core.IdentityProvider

And add an implicit `RequestHeader` parameter to it too.  For example:

	:::scala
	@(user: securesocial.core.SocialUser)(implicit request: RequestHeader)


### Java

For Java applications the syntax is a little bit different.  Assuming your template received a `user` parameter with an instance of `SocialUser` you would add:

 	:::html
 	@if( user.passwordInfo != null ) {
        <a class="btn" href="@securesocial.core.providers.utils.RoutesHelper.changePasswordPage.absoluteURL(Implicit.request(), IdentityProvider.sslEnabled)">
        	Change Password
        </a>
    }

Make sure to add the following import statements in your template:

	:::java
	@import securesocial.core.IdentityProvider
	@import Http.Context.Implicit    

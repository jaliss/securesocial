package controllers;

import play.mvc.*;
import securesocial.controllers.java.BaseLoginPage;

public class MyLoginPage extends BaseLoginPage {
	
	public static Result login() {
		return ok(views.html.login.render(getProviders()));
	}
	
	public static Result logout() {
		return BaseLoginPage.logout();
	}
	
	public static Result authenticate(final String providerId) {
		return BaseLoginPage.authenticate(providerId);
	}
}
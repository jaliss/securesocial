package controllers;

import controllers.securesocial.SecureSocialLogin;
import play.mvc.Controller;
import play.mvc.With;

@With( SecureSocialLogin.class )
public class Application extends Controller {
    public static void index() {
        render();
    }
}
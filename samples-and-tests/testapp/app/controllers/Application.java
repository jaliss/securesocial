package controllers;

import controllers.securesocial.SecureSocial;
import play.mvc.Controller;
import play.mvc.With;

@With( SecureSocial.class )
public class Application extends Controller {
    public static void index() {
        render();
    }
}
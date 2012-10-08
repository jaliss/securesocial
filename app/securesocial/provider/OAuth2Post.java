package securesocial.provider;

import java.util.HashMap;
import java.util.Map;

import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

import play.libs.OAuth2;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import com.google.gson.JsonObject;
/**
 * Copy of Play's OAuth2 but with post instead of get.  
 *
 */
public class OAuth2Post extends OAuth2 {

	//we need local copies because of property enhancer.  play is not designed for subclassing!
    public String authorizationURL;
    public String accessTokenURL;
    public String clientid;
    public String secret;

    public OAuth2Post(String authorizationURL,
            String accessTokenURL,
            String clientid,
            String secret) {
    	super(authorizationURL, accessTokenURL, clientid, secret);
    	this.accessTokenURL = accessTokenURL;
        this.authorizationURL = authorizationURL;
        this.clientid = clientid;
        this.secret = secret;
    }

    public Response retrieveAccessToken(String callbackURL) {
        String accessCode = Params.current().get("code");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("client_id", clientid);
        params.put("client_secret", secret);
        params.put("redirect_uri", callbackURL);
        params.put("code", accessCode);
        HttpResponse response = WS.url(accessTokenURL).params(params).post();
        return new Response(response);
    }

}

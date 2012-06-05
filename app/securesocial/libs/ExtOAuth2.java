package securesocial.libs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import play.exceptions.UnexpectedException;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.Scope;
import play.mvc.results.Redirect;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Extended version of OAuth2 class providing the followed improvment :
 * <ul>
 * <li>Choose between GET/POST to request tokenURL (default is GET)</li>
 * <li>Allow extra parameters to request authorizationURL (scope, ...)</li>
 * <li>Allow extra parameters to request tokenURL (scope, ...)</li>
 * </ul>
 *
 * @author cspada
 */
public class ExtOAuth2 extends OAuth2 {

    // Json response properties
    private static final String ACCESS_TOKEN = "access_token";

    private static final String OAUTH_TOKEN = "oauth_token";

    public static final String GET_METHOD = "get";

    public static final String POST_METHOD = "post";

    public String accessTokenURLMethod = GET_METHOD;

    public Map<String, Object> authURLParams = new HashMap<String, Object>();

    public Map<String, Object> tokenURLParams = new HashMap<String, Object>();

    public ExtOAuth2(String authorizationURL, String accessTokenURL, String clientid, String secret) {
        super(authorizationURL, accessTokenURL, clientid, secret);
    }

    public void addAuthorizationURLExtraParam(String param, Object value) {
        this.authURLParams.put(param, value);
    }

    public void addTokenURLExtraParam(String param, Object value) {
        this.tokenURLParams.put(param, value);
    }

    /**
     * First step of the OAuth2 process: redirects the user to the authorization page
     *
     * @param callbackURL
     */
    public void retrieveVerificationCode(String callbackURL) {
        authURLParams.put("client_id", clientid);
        authURLParams.put("redirect_uri", callbackURL);
        String url = buildURL(authorizationURL, authURLParams);
        throw new Redirect(url);
    }

    public Response retrieveAccessToken(String callbackURL) {
        String accessCode = Scope.Params.current().get("code");
        tokenURLParams.put("client_id", clientid);
        tokenURLParams.put("client_secret", secret);
        tokenURLParams.put("redirect_uri", callbackURL);
        tokenURLParams.put("code", accessCode);
        tokenURLParams.put("grant_type", "authorization_code");
        WS.HttpResponse response = POST_METHOD.equalsIgnoreCase(accessTokenURLMethod) ?
                WS.url(accessTokenURL).params(tokenURLParams).post() :
                WS.url(accessTokenURL).params(tokenURLParams).get();
        return new Response(response);
    }

    private String buildURL(String url, Map<String, Object> params) {
        try {
            StringBuilder sb = new StringBuilder(url);
            if (params != null && !params.isEmpty()) {
                boolean first = true;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    sb.append(first ? "?" : "&");
                    sb.append(entry.getKey()).append("=")
                            .append(URLEncoder.encode(String.valueOf(entry.getValue()), "utf-8"));
                    first = false;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Extract the access_token from the given Response (which should be a Json response).
     * @param response
     * @return
     */
    public String extractAccessToken(Response response) {
        if (response.error != null) {
            if (response.error.type == OAuth2.Error.Type.UNKNOWN) {
                // the OAuth2 class is expecting the access token in the query string.
                // this is not what the OAuth2 spec says.  Facebook works, but Foursquare fails for example.
                // So check if the token is there before throwing the exception.
                JsonElement asJson = response.httpResponse.getJson();

                if (asJson != null) {
                    JsonObject body = asJson.getAsJsonObject();
                    if (body != null) {
                        // this is what many libraries expect (probably because Facebook returns it)
                        JsonElement token = body.get(ACCESS_TOKEN);
                        if (token != null) {
                            return token.getAsString();
                        } else {
                            // this is what should be returned as defined in the OAuth2 spec
                            token = body.get(OAUTH_TOKEN);
                            if (token != null) {
                                return token.getAsString();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }


    public String getClientid() {
        return this.clientid;
    }

    public String getAuthorizationURL() {
        return this.authorizationURL;
    }

    public String getSecret() {
        return this.secret;
    }

    public String getAccessTokenURL() {
        return this.accessTokenURL;
    }
}

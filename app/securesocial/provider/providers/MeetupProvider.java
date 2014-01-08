package securesocial.provider.providers;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.libs.OAuth2;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import securesocial.libs.ExtOAuth2;
import securesocial.provider.AuthenticationException;
import securesocial.provider.OAuth2Provider;
import securesocial.provider.ProviderType;
import securesocial.provider.SocialUser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MeetupProvider extends OAuth2Provider {

	private static final ProviderType meetupType = ProviderType.meetup;

	// this is a util class for safely (no NPEs) extracting from a json tree.
	public static class JsonUtil {
		public static String getString(JsonElement json, String... path) {
			JsonElement e = getPath(json, path);
			if (e == null)
				return "";
			else {
				if (e.isJsonPrimitive())
					return e.getAsString();
				else {
					return new Gson().toJson(e);
				}
			}
		}

		public static JsonElement getPath(JsonElement json, String... path) {
			try {
				JsonElement jsonElement = json;
				for (String p : path)
					jsonElement = jsonElement.getAsJsonObject().get(p);
				if (jsonElement != null)
					return jsonElement;
			} catch (Exception e) {
				Logger.warn("Could not get path" + StringUtils.join(path) + " "
						+ e.getMessage());
			}

			return null;
		}
	}

	static final String ME = "https://api.meetup.com/2/member/self";
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String EMAIL = "email";
	private static final String PHOTO = "photo";
	private static final String PHOTO_LINK = "photo_link";
	private static final String ERROR = "problem";
	private static final String MESSAGE = "details";

	public MeetupProvider() {
		super(meetupType);
	}

	@Override
	protected void fillProfile(SocialUser user, Map<String, Object> authContext) {
		Logger.info("Filling meetup profile");
		HttpResponse httpResponse = WS.url(ME)
				.setParameter("access_token", user.accessToken).get();
		String json = httpResponse.getString();
		// Logger.debug(json);
		JsonObject me = new JsonParser().parse(json).getAsJsonObject();
		JsonObject error = me.getAsJsonObject(ERROR);

		if (error != null) {
			final String message = error.get(MESSAGE).getAsString();
			final String type = error.get(ERROR).getAsString();
			Logger.error(
					"Error retrieving profile information from Meetup. Error type: %s, message: %s.",
					type, message);
			throw new AuthenticationException();
		}

		user.id.id = JsonUtil.getString(me, ID);
		user.displayName = JsonUtil.getString(me, NAME);
		user.avatarUrl = JsonUtil.getString(me, PHOTO, PHOTO_LINK);
		user.email = JsonUtil.getString(me, EMAIL);
	}

	@Override
	protected OAuth2 createOAuth2(String key) {
		// this uses a POST version of OAuth2 (OAuth2Post) instead of the
		// default GET OAuth2. This is required by meetup.com's auth process
		// See: http://www.meetup.com/meetup_api/auth/#oauth2server-access
		ExtOAuth2 oauth2Service = new ExtOAuth2(
				Play.configuration.getProperty(key + AUTHORIZATION_URL),
				Play.configuration.getProperty(key + ACCESS_TOKEN_URL),
				Play.configuration.getProperty(key + CLIENTID),
				Play.configuration.getProperty(key + SECRET));
		oauth2Service.accessTokenURLMethod = ExtOAuth2.POST_METHOD;
		return oauth2Service;
	}

}

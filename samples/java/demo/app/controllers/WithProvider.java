package controllers;

import securesocial.core.java.Authorization;
import securesocial.core.java.SocialUser;

/**
 * A sample authorization implementation that lets you filter requests based
 * on the provider that authenticated the user
 */
public class WithProvider implements Authorization {
    public boolean isAuthorized(SocialUser user, String params[]) {
        return user.id.provider.equals(params[0]);
    }
}

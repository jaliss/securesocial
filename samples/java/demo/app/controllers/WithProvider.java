package controllers;

import securesocial.core.Identity;
import securesocial.core.java.Authorization;

/**
 * A sample authorization implementation that lets you filter requests based
 * on the provider that authenticated the user
 */
public class WithProvider implements Authorization {
    public boolean isAuthorized(Identity user, String params[]) {
        return user.id().providerId().equals(params[0]);
    }
}

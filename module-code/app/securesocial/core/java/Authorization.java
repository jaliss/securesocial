package securesocial.core.java;

import securesocial.core.Identity;

/**
 * The interface that defines the authorization implementation.
 */
public interface Authorization {
    boolean isAuthorized(Identity user, String[] params);
}

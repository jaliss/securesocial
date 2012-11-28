package securesocial.core.java;

/**
 *
 */
public interface Authorization {
    boolean isAuthorized(securesocial.core.java.SocialUser user, String[] params);
}

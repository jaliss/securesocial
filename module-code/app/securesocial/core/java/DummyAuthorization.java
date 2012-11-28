package securesocial.core.java;

/**
 * A dummy authorization that just allows all executions. Used when no custom Authorization implementation
 * is specified.
 */
class DummyAuthorization implements Authorization {
    @Override
    public boolean isAuthorized(SocialUser user, String[] params) {
        return true;
    }
}
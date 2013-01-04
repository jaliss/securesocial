package securesocial.core.java;

import play.mvc.Http;
import securesocial.core.Identity;

/**
 * A dummy authorization that just allows all executions. Used when no custom Authorization implementation
 * is specified.
 */
class DummyAuthorization implements Authorization {
    @Override
    public boolean isAuthorized(Identity user, String[] params) {
        return true;
    }
}
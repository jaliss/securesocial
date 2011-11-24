/**
 * Copyright 2011 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package controllers;

import controllers.deadbolt.DeadboltHandler;
import controllers.deadbolt.ExternalizedRestrictionsAccessor;
import controllers.deadbolt.RestrictedResourcesHandler;
import controllers.securesocial.SecureSocial;
import models.MyRole;
import models.MyRoleHolder;
import models.deadbolt.Role;
import models.deadbolt.RoleHolder;
import play.mvc.Controller;
import securesocial.provider.SocialUser;

import java.util.ArrayList;
import java.util.List;

/**
 * A sample handler to show SecureSocial and Deadbolt integration
 */
public class MyDeadboltHandler extends Controller implements DeadboltHandler {

    public void beforeRoleCheck() {
        try {
            SecureSocial.DeadboltHelper.beforeRoleCheck();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    public RoleHolder getRoleHolder() {
        // get the current user
        SocialUser user = SecureSocial.getCurrentUser();

        // create a role based on the network the user belongs to.
        List<Role> roles = new ArrayList<Role>();
        roles.add(new MyRole( user.id.provider.toString()));

        // we're done
        return new MyRoleHolder(roles);
    }

    public void onAccessFailure(String controllerClassName) {
        forbidden();
    }

    public ExternalizedRestrictionsAccessor getExternalizedRestrictionsAccessor() {
        return null;
    }

    public RestrictedResourcesHandler getRestrictedResourcesHandler() {
        return null;  
    }
}

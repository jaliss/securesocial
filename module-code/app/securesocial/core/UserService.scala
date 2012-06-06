/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
 */
package securesocial.core

import play.api.{Logger, Plugin, Application}


/**
 * A trait that provides the means to find and save users
 * for the SecureSocial module.
 *
 * @see DefaultUserService
 */
trait UserService {
  /**
   * Finds a SocialUser that maches the specified id
   *
   * @param id the user id
   * @return an optional user
   */
  def find(id: UserId):Option[SocialUser]

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   * @param user
   */
  def save(user: SocialUser)
}

/**
 * Base class for the classes that implement UserService.  Since this is a plugin it gets loaded
 * at application start time.  Only one plugin of this type must be specified in the play.plugins file.
 *
 * @param application
 */
abstract class UserServicePlugin(application: Application) extends Plugin with UserService {
  /**
   * Registers this object so SecureSocial can invoke it.
   */
  override def onStart() {
    UserService.setService(this)
    Logger.info("Registered UserService: " + this.getClass)
  }
}

/**
 * The UserService singleton
 */
object UserService {
  var delegate: Option[UserService] = None

  def setService(service: UserService) {
    delegate = Some(service)
  }

  def find(id: UserId):Option[SocialUser] = {
    delegate.map( _.find(id) ).getOrElse {
      notInitialized()
      None
    }
  }

  def save(user: SocialUser) {
    delegate.map( _.save(user) ).getOrElse {
      notInitialized()
    }
  }

  private def notInitialized() {
    Logger.error("UserService was not initialized. Make sure a UserService plugin is specified in your play.plugins file")
    throw new RuntimeException("UserService not initialized")
  }
}

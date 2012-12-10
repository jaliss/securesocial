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
package securesocial.core.providers.utils

import securesocial.core.{Registry, Registrable, PasswordInfo}
import play.api.{Logger, Plugin, Application}
import org.mindrot.jbcrypt._

/**
 * A trait that defines the password hasher interface
 */

trait PasswordHasher extends Plugin with Registrable {

  override def onStart() {
    Logger.info("[securesocial] loaded password hasher %s".format(id))
    Registry.hashers.register(this)
  }


  override def onStop() {
    Logger.info("[securesocial] unloaded password hasher %s".format(id))
    Registry.hashers.unRegister(id)
  }
  /**
   * Hashes a password
   *
   * @param plainPassword the password to hash
   * @return a PasswordInfo containting the hashed password and optional salt
   */
  def hash(plainPassword: String): PasswordInfo

  /**
   * Checks whether a supplied password matches the hashed one
   *
   * @param passwordInfo the password retrieved from the backing store (by means of UserService)
   * @param suppliedPassword the password supplied by the user trying to log in
   * @return true if the password matches, false otherwise.
   */
  def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean
}

object PasswordHasher {
  val BCryptHasher = "bcrypt"
}

/**
 * The default password hasher based on BCrypt.
 */
class BCryptPasswordHasher(app: Application) extends PasswordHasher {
  val DefaultRounds = 10
  val RoundsProperty = "securesocial.passwordHasher.bcrypt.rounds"

  override def id = PasswordHasher.BCryptHasher

  /**
   * Hashes a password. This implementation does not return the salt because it is not needed
   * to verify passwords later.  Other implementations might need to return it so it gets saved in the
   * backing store.
   *
   * @param plainPassword the password to hash
   * @return a PasswordInfo containing the hashed password.
   */
  def hash(plainPassword: String): PasswordInfo = {
    val logRounds = app.configuration.getInt(RoundsProperty).getOrElse(DefaultRounds)
    PasswordInfo(id, BCrypt.hashpw(plainPassword, BCrypt.gensalt(logRounds)))
  }

  /**
   * Checks if a password matches the hashed version
   *
   * @param passwordInfo the password retrieved from the backing store (by means of UserService)
   * @param suppliedPassword the password supplied by the user trying to log in
   * @return true if the password matches, false otherwise.
   */
  def matches(passwordInfo: PasswordInfo, suppliedPassword: String):Boolean = {
    BCrypt.checkpw(suppliedPassword, passwordInfo.password)
  }
}
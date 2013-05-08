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

import play.api.{Plugin, Application}
import play.api.i18n.Messages

/**
 * A trait to define password validators.
 */
abstract class PasswordValidator extends Plugin {
  /**
   * Returns true if the password is valid
   *
   * @param password the password to check
   * @return true if the password is valid, false otherwise
   */
  def isValid(password: String): Boolean

  /**
   * An error message shown in the sign up page if the password is not good
   * enough for this validator
   * @return
   */
  def errorMessage: String
}

/**
 * A default password validator that only checks a mnimum length.
 * The minimum length can be configured setting a minimumPasswordLength property for userpass.
 * Defaults to 8 if not specified.
 */
class DefaultPasswordValidator(application: Application) extends PasswordValidator {
  import DefaultPasswordValidator._

  private def requiredLength = application.configuration.getInt(PasswordLengthProperty).getOrElse(DefaultLength)
  def isValid(password: String): Boolean = password.length >= requiredLength
  def errorMessage = Messages("securesocial.signup.invalidPassword", requiredLength)
}

object DefaultPasswordValidator {
  val PasswordLengthProperty = "securesocial.userpass.minimumPasswordLength"
  val DefaultLength = 8
}


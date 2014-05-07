/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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

import play.api.data.validation.{Constraint, Invalid, Valid}
import securesocial.core.RuntimeEnvironment

/**
 * A trait to define password validators.
 */
trait PasswordValidator {
  /**
   * Validates a password
   *
   * @param password the supplied password
   * @return Right if the password is valid or Left with an error message otherwise
   */
  def validate(password: String): Either[(String, Seq[Any]), Unit]
}

object PasswordValidator {
  /**
   * A helper method to create a constraint used in forms
   *
   * @param env a RuntimeEnvironment with the PasswordValidator implmentation to use
   * @return Valid if the password is valid or Invalid otherwise
   */
  def constraint(implicit env: RuntimeEnvironment[_]) = Constraint[String] {s: String =>
    env.passwordValidator.validate(s) match {
      case Right(_) => Valid
      case Left (error) => Invalid (error._1, error._2: _*)
    }
  }

  /**
   * A default password validator that only checks a minimum length.
   *
   * The minimum length can be configured setting a minimumPasswordLength property for userpass.
   * Defaults to 8 if not specified.
   */
  class Default(requiredLength: Int) extends PasswordValidator {
    def this() = this({
      val app = play.api.Play.current
      app.configuration.getInt(Default.PasswordLengthProperty).getOrElse(Default.Length)
    })

    override def validate(password: String): Either[(String, Seq[Any]), Unit] = {
      if ( password.length >= requiredLength ) {
        Right(())
      } else
        Left((Default.InvalidPasswordMessage, Seq(requiredLength)))
    }
  }

  object Default {
    val Length = 8
    val PasswordLengthProperty = "securesocial.userpass.minimumPasswordLength"
    val InvalidPasswordMessage = "securesocial.signup.invalidPassword"
  }
}

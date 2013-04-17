/**
 * Copyright 2013 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
import org.specs2.mutable.Specification
import play.api.test._
import securesocial.core.providers.utils.BCryptPasswordHasher

class BCryptPasswordHasherSpec extends Specification {
  val hasher = new BCryptPasswordHasher(new FakeApplication())
  val password = "my_secret_password"
  val pinfo = hasher.hash(password)

  "BCryptPasswordHasher" should {
    "hash a password" in {
      pinfo.hasher == hasher.id
      pinfo.password.length > 0
    }

    "match a correct password" in {
      hasher.matches(pinfo, password)
    }

    "not match an invalid password" in {
      hasher.matches(pinfo, "a_wrong_password") == false
    }
  }
}

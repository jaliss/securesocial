/**
 * Copyright 2013-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core.authenticator

import scala.concurrent.Future
import java.security.SecureRandom
import play.api.libs.Codecs

/**
 * An Authenticator Id generator.
 */
trait IdGenerator {
  def generate: Future[String]
}

object IdGenerator {
  /**
   * The default id generator
   */
  class Default extends IdGenerator {
    //todo: this needs improvement, several threads will wait for the synchronized block in SecureRandom.
    // I will probably need a pool of SecureRandom instances.
    val random = new SecureRandom()
    val DefaultSizeInBytes = 128
    val IdLengthKey = "securesocial.idLengthInBytes"
    val IdSizeInBytes = play.api.Play.current.configuration.getInt(IdLengthKey).getOrElse(DefaultSizeInBytes)

    /**
     * Generates a new id using SecureRandom
     *
     * @return the generated id
     */
    def generate: Future[String] = {
      //todo: review the usage of future here
      Future.successful {
        var randomValue = new Array[Byte](IdSizeInBytes)
        random.nextBytes(randomValue)
        Codecs.toHexString(randomValue)
      }
    }
  }
}
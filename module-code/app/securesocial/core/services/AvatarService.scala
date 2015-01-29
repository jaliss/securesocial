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
package securesocial.core.services

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * A mockable interface for the avatar service
 */
trait AvatarService {
  def urlFor(userId: String): Option[String]
}

object AvatarService {

  /**
   * A default implemtation
   * @param httpService
   */
  class Default(httpService: HttpService) extends AvatarService {
    import _root_.java.security.MessageDigest

    private val logger = play.api.Logger("securesocial.core.providers.utils.AvatarService.Default")

    val GravatarUrl = "http://www.gravatar.com/avatar/%s?d=404"
    val Md5 = "MD5"

    override def urlFor(userId: String): Option[String] = {
      if (userId.trim.toLowerCase.isEmpty) {
        None;
      } else {
        val avatarUrl = GravatarUrl.format(hash(userId))
        val response =  Await.result(httpService.url(avatarUrl).get(),3.second)
        if (response.status == 200) {
          Some(avatarUrl)
        } else {
          None;
        }
      }
    }

    private def hash(email: String): Option[String] = {
      val s = email.trim.toLowerCase
      val out = MessageDigest.getInstance(Md5).digest(s.getBytes)
      Some(BigInt(1, out).toString(16))
    }
  }
}

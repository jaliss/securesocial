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

import scala.concurrent.Future


/**
 * An interface for the Cache API
 */
trait CacheService {

  import scala.reflect.ClassTag

  def set[T](key: String, value: T, ttlInSeconds: Int = 0): Future[Unit]

  def getAs[T](key: String)(implicit ct: ClassTag[T]): Future[Option[T]]

  def remove(key: String): Future[Unit]
}

object CacheService {

  /**
   * A default implementation for the CacheService based on the Play cache.
   */
  class Default extends CacheService {
    import play.api.cache.Cache
    import scala.reflect.ClassTag
    import play.api.Play.current

    override def set[T](key: String, value: T, ttlInSeconds: Int): Future[Unit] =
      Future.successful(Cache.set(key, value))

    override def getAs[T](key: String)(implicit ct: ClassTag[T]): Future[Option[T]] = Future.successful {
      Cache.getAs[T](key)
    }

    override def remove(key: String): Future[Unit] = Future.successful {
      Cache.remove(key)
    }
  }
}

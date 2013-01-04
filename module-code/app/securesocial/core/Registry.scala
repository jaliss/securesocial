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

import play.api.Logger
import providers.UsernamePasswordProvider
import providers.utils.PasswordHasher
import scala.None


trait Registrable {
  def id: String
}

class PluginRegistry[T <: Registrable](label: String) {
  private var registry = Map[String, T]()

  def register(plugin: T) {
    if ( registry.contains(plugin.id) ) {
      throw new RuntimeException("There is already a %s registered with id %s".format(label, plugin.id))
    }

    val p = (plugin.id, plugin)
    registry += p
  }

  def unRegister(id: String) {
    registry -= id
  }

  def get(id: String): Option[T] = registry.get(id) orElse {
    Logger.error("[securesocial] can't find %s for id %s".format(label, id))
    None
  }

  def all() = registry
}

/**
 * A registry for providers and password hashers.  Providers and password hashers register themselves
 * here when they are loaded by Play
 */
object Registry {
  lazy val providers = new PluginRegistry[IdentityProvider]("provider")
  lazy val hashers = new PluginRegistry[PasswordHasher]("password hasher") {
    lazy val currentHasher: PasswordHasher = get(UsernamePasswordProvider.hasher).get
  }
}
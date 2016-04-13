/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
  * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import controllers.CustomRoutesService
import securesocial.core.RuntimeEnvironment
import service.{DemoUser, InMemoryUserService, MyEventListener}

/**
  * The runtime environment for this sample app.
  */
class MyRuntimeEnvironment extends RuntimeEnvironment.Default {
  type U = DemoUser
  override implicit val executionContext = play.api.libs.concurrent.Execution.defaultContext
  override lazy val routes = new CustomRoutesService()
  override lazy val userService: InMemoryUserService = new InMemoryUserService()
  override lazy val eventListeners = List(new MyEventListener())
}

/**
  * An implementation that checks if the controller expects a RuntimeEnvironment and
  * passes the instance to it if required.
  *
  * This can be replaced by any DI framework to inject it differently.
  *
  * @param controllerClass
  * @tparam A
  * @return
  */
/* def getControllerInstance[A](controllerClass: Class[A]): A = {
  val instance = controllerClass.getConstructors.find { c =>
    val params = c.getParameterTypes
    params.length == 1 && params(0) == classOf[RuntimeEnvironment[DemoUser]]
  }.map {
    _.asInstanceOf[Constructor[A]].newInstance(MyRuntimeEnvironment)
  }
  instance.getOrElse(super.getControllerInstance(controllerClass))
}*/ 
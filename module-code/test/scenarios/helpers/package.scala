package scenarios

import java.lang.reflect.Constructor
import play.api.test.{Writeables, RouteInvokers, PlayRunners}
import securesocial.core.RuntimeEnvironment
import securesocial.core.services.{UserService, RoutesService}

package object helpers {
  type ApiExecutor = PlayRunners with RouteInvokers with Writeables

  class TestGlobal[U]( var runtimeEnvironment:RuntimeEnvironment[U]) extends play.api.GlobalSettings {

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
    override def getControllerInstance[A](controllerClass: Class[A]): A = {
      val instance  = controllerClass.getConstructors.find { c =>
        val params = c.getParameterTypes
        params.length == 1 && params(0) == classOf[RuntimeEnvironment[U]]
      }.map {
        _.asInstanceOf[Constructor[A]].newInstance(runtimeEnvironment)
      }
      instance.getOrElse(super.getControllerInstance(controllerClass))
    }
  }
}

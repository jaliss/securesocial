package securesocial.core.providers

import play.Play

package object utils {
  val RoutesHelper = {
    lazy val conf = play.api.Play.current.configuration
    lazy val rc = {
      val clazz = conf.getString("securesocial.routesHelper.class").getOrElse("securesocial.core.providers.utils.DefaultRoutesHelper")
      Play.application().classloader().loadClass(clazz)
    }
    rc.newInstance().asInstanceOf[DefaultRoutesHelper]
  }
}

import play.api.{Application, Plugin}

package object securesocial {
  def use[A <: Plugin](implicit app: Application, m: Manifest[A]) = {
    app.plugin[A].getOrElse(throw new RuntimeException(m.runtimeClass.toString + " plugin should be available at this point"))
  }
}




import play.api.{ Configuration, Environment }
import play.api.inject.{ Binding, Module }
import securesocial.core.RuntimeEnvironment
import service.MyEnvironment

class DemoModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[RuntimeEnvironment].to[MyEnvironment])
  }
}

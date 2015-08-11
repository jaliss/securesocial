package scenarios.helpers

import javax.inject.{ Provider, Inject }

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import play.api.http.{ HttpErrorHandler, HttpRequestHandler }
import play.api.inject._
import play.api.routing.Router
import play.api._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ RequestHeader, Handler }
import play.api.test._

import securesocial.core.{ BasicProfile, RuntimeEnvironment }

import scala.concurrent.Future
import scala.runtime.AbstractPartialFunction

/**
 * Created by dverdone on 8/6/15.
 */
class TestBindingModule extends AbstractModule {
  override def configure(): Unit = {
    //  bind(classOf[RuntimeEnvironment.Default[BasicProfile]]).to(classOf[TestGlobal])
  }

}

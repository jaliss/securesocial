package scenarios.helpers

import play.api.Application
import play.api.test.{ FakeRequest, PlayRunners, RouteInvokers, Writeables }

trait SocialProviders {
  self: ApiExecutor =>
  def authenticate(provider: String, redirectTo: Option[String] = None)(implicit app: Application) = {
    val redirectParam = redirectTo.map(s => s"redirectTo=$s")
    val params = List(redirectParam).flatten.mkString("?", "&", "")
    route(app, FakeRequest(GET, s"/authenticate/google$params")).get
  }
  def callback(provider: String, state: Option[String] = None, code: Option[String] = None, redirectTo: Option[String] = None, session: Seq[(String, String)] = Nil)(implicit app: Application) = {
    val redirectParam = redirectTo.map(s => s"redirectTo=$s")
    val stateParam = state.map(s => s"state=$s")
    val codeParam = code.map(s => s"code=$s")
    val params = List(redirectParam, stateParam, codeParam).flatten.mkString("?", "&", "")
    route(app, FakeRequest(GET, s"/authenticate/google$params").withSession(session: _*)).get
  }
}

object Api extends SocialProviders with PlayRunners with RouteInvokers with Writeables

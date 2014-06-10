package scenarios.helpers

import play.api.test.{PlayRunners, Writeables, RouteInvokers, FakeRequest}
import play.api.Logger
import securesocial.Routes

trait SocialProviders{
  self:ApiExecutor =>
  def authenticate(provider:String, redirectTo: Option[String]=None)={
    val redirectParam = redirectTo.map(s=>s"redirectTo=$s")
    val params = List(redirectParam).flatten.mkString("?","&","")
    route(FakeRequest(GET,s"/authenticate/google$params")).get
  }
  def callback(provider:String, state:Option[String]=None, code:Option[String]=None,redirectTo: Option[String]=None, session:Seq[(String, String)] = Nil)={
    val redirectParam = redirectTo.map(s=>s"redirectTo=$s")
    val stateParam = state.map(s=>s"state=$s")
    val codeParam = code.map(s=>s"code=$s")
    val params = List(redirectParam, stateParam, codeParam).flatten.mkString("?","&","")
    route(FakeRequest(GET,s"/authenticate/google$params").withSession(session:_*)).get
  }
}

object Api extends SocialProviders with PlayRunners with RouteInvokers with Writeables

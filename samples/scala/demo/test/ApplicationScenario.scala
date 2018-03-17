import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Cookies
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import service.CustomProviders

class ApplicationScenario extends PlaySpecification {
  def app: Application = new GuiceApplicationBuilder()
    .bindings(bind[CustomProviders].to(CustomProviders(Seq(new NaiveIdentityProvider))))
    .build()

  "A logged in user can view the index" in new WithApplication(app) {
    //Given
    val creds1: Cookies = cookies(route(app, FakeRequest(POST, "/auth/authenticate/naive").withTextBody("user")).get)
    //Whenid
    val Some(response) = route(app, FakeRequest(GET, "/").withCookies(creds1("id")))
    //Then
    status(response) must equalTo(OK)
  }
}

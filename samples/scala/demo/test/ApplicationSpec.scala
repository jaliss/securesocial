import org.specs2.matcher.ShouldMatchers
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ AnyContent, Cookies, Request }
import play.api.test.{ FakeRequest, Injecting, PlaySpecification, WithApplication }
import service.CustomProviders

class ApplicationSpec extends PlaySpecification with ShouldMatchers {
  def app: Application = new GuiceApplicationBuilder()
    .bindings(bind[CustomProviders].to(CustomProviders(Seq(new NaiveIdentityProvider))))
    .build()

  "Access secured index " in new WithApplication(app) with Injecting {
    val controller = inject[controllers.Application]

    // same thing we do in ApplicationScenario
    val allCookies: Cookies =
      cookies(route(app, FakeRequest(POST, "/auth/authenticate/naive").withTextBody("user")).get)
    val authCookie = allCookies("id")

    val req: Request[AnyContent] = FakeRequest().
      withHeaders((HeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded")).
      withCookies(authCookie)
    val result = controller.index(req)
    val actual: Int = status(result)
    actual must be equalTo OK
  }
}
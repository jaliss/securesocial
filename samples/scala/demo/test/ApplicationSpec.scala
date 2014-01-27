import controllers.Application
import org.specs2.matcher.ShouldMatchers
import play.api.http.HeaderNames
import play.api.mvc.{Request, AnyContent}
import play.api.test.{PlaySpecification, FakeApplication, FakeRequest}
import securesocial.testkit.WithLoggedUser

class ApplicationSpec extends PlaySpecification with ShouldMatchers {
  import WithLoggedUser._
  def minimalApp = FakeApplication(withoutPlugins=excludedPlugins,additionalPlugins = includedPlugins)
  "Access secured index " in new WithLoggedUser(minimalApp) {

    val req: Request[AnyContent] = FakeRequest().
      withHeaders((HeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded")).
      withCookies(cookie) // Fake cookie from the WithloggedUser trait

    val result = Application.index.apply(req)

    val actual: Int= status(result)
    actual must be equalTo OK
  }
}

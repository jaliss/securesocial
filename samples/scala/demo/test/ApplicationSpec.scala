import controllers.CustomRoutesService
import org.joda.time.DateTime
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito
import play.api.http.HeaderNames
import play.api.mvc.{AnyContent, Request}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import securesocial.core.RuntimeEnvironment
import securesocial.core.services.AuthenticatorService
import securesocial.testkit.{BasicProfileGenerator, FakeAuthenticator}
import service.{DemoUser, InMemoryUserService, MyEventListener}

import scala.concurrent.Future

class ApplicationSpec extends PlaySpecification with ShouldMatchers with Mockito {
  class LoggedRuntimeEnvironment(mockAuthenticatorService:AuthenticatorService[DemoUser]) extends RuntimeEnvironment.Default[DemoUser] {
    override lazy val routes = new CustomRoutesService()
    override lazy val userService: InMemoryUserService = new InMemoryUserService()
    override lazy val eventListeners = List.empty
    override lazy val authenticatorService = mockAuthenticatorService
  }
  "Access secured index " in new WithApplication() {
      val loggedinUser=DemoUser(BasicProfileGenerator.basicProfile(),List.empty)
      val mockAuthenticator = mock[AuthenticatorService[DemoUser]]
      val env = new LoggedRuntimeEnvironment(mockAuthenticator)
      val req: Request[AnyContent] = FakeRequest().
        withHeaders((HeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded"))

      mockAuthenticator.fromRequest(req) returns Future.successful(Some(asAuthenticator(loggedinUser)))

      val result = new controllers.Application()(env).index.apply(req)

      val actual: Int= status(result)
      actual must be equalTo OK
  }

  private def asAuthenticator(user:DemoUser): FakeAuthenticator[DemoUser] = {
    val now = DateTime.now
    FakeAuthenticator(user.main.userId, user, now.minusSeconds(1),now.plusDays(1),now)
  }
}

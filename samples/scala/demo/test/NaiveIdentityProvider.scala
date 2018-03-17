import play.api.mvc.{ AnyContent, Request }
import securesocial.core.{ AuthenticationMethod, AuthenticationResult, BasicProfile, IdentityProvider }

import scala.concurrent.Future

class NaiveIdentityProvider extends IdentityProvider {
  override val id: String = "naive"
  override def authMethod: AuthenticationMethod = AuthenticationMethod("naive")
  override def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    val userId = request.body.toString
    Future.successful(AuthenticationResult.Authenticated(BasicProfile(
      id,
      userId,
      Some("Foo"),
      Some("Bar"),
      Some("Foo Bar"),
      Some("foo.bar@example.com"),
      None,
      authMethod)))
  }
}

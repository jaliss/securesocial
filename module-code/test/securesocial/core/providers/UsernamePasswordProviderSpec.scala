package securesocial.core.providers

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc._
import play.api.test._
import play.twirl.api.Html
import securesocial.controllers.ViewTemplates
import securesocial.core.AuthenticationResult.Authenticated
import securesocial.core.{ PasswordInfo, BasicProfile, AuthenticationResult }
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.services._

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class UsernamePasswordProviderSpec extends PlaySpecification with Mockito {
  "UsernamePasswordProvider" should {
    "fail auth with BadRequest if form has errors" in new WithMocks {
      val resFut = upp.authenticate()(FakeRequest())
      await(resFut) match {
        case AuthenticationResult.NavigationFlow(x) => x.header.status must_== BAD_REQUEST
        case t => failure(t.toString)
      }
    }

    "fail auth with BadRequest if credentials are not valid" in new WithMocks {
      val form = FakeRequest().withFormUrlEncodedBody("username" -> "foo@bar.com", "password" -> "wrong password")
      val resFut = upp.authenticate()(form)
      await(resFut) match {
        case AuthenticationResult.NavigationFlow(x) => x.header.status must_== BAD_REQUEST
        case t => failure(t.toString)
      }
    }

    "Authenticate if password is valid for user" in new WithMocks {
      val form = FakeRequest().withFormUrlEncodedBody("username" -> "foo@bar.com", "password" -> "password")
      val resFut = upp.authenticate()(form)
      await(resFut) match {
        case Authenticated(_) => success
        case t => failure(t.toString)
      }
    }

    "[Issue 465] Authenticate if password is valid for user and AvatarService is None" in new WithMocks {
      val nonAvatar = new UsernamePasswordProvider(userService, None, viewTemplates, passwordHashers)
      val form = FakeRequest().withFormUrlEncodedBody("username" -> "foo@bar.com", "password" -> "password")
      val resFut = nonAvatar.authenticate()(form)
      await(resFut) match {
        case Authenticated(_) => success
        case t => failure(t.toString)
      }
    }
  }

  trait WithMocks extends Before with Mockito {
    val userService = mock[UserService[User]]
    val avatarService = mock[AvatarService]
    val viewTemplates = mock[ViewTemplates]
    val passwordHashers = mock[Map[String, PasswordHasher]]
    val upp = new UsernamePasswordProvider(userService, Some(avatarService), viewTemplates, passwordHashers)

    def before = {
      viewTemplates.getLoginPage(any[Form[(String, String)]], any[Option[String]])(any[RequestHeader], any[Lang]) returns Html("login page")
      userService.find(upp.id, "foo@bar.com") returns Future(Some(basicProfileFor(User("foo@bar.com", "password"))))
      passwordHashers.get("bcrypt") returns Some(new PasswordHasher.Default(12))
      avatarService.urlFor("foo@bar.com") returns Future(None)
    }

    def basicProfileFor(user: User) = BasicProfile(
      providerId = upp.id,
      userId = user.email,
      firstName = None,
      lastName = None,
      fullName = None,
      email = Some(user.email),
      avatarUrl = None,
      authMethod = upp.authMethod,
      oAuth1Info = None,
      oAuth2Info = None,
      passwordInfo = Some(PasswordInfo("bcrypt", user.hash))
    )
  }

  case class User(
      email: String,
      password: String) {
    import org.mindrot.jbcrypt.BCrypt
    val hash = BCrypt.hashpw(password, BCrypt.gensalt(12))
  }
}

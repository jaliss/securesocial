package securesocial.testkit

import securesocial.core.{Authenticator, Identity, UserService}
import org.specs2.execute.{Result, AsResult}
import play.api.test.{WithApplication, FakeApplication}
import org.specs2.mock.Mockito

abstract class WithLoggedUser(override val app: FakeApplication = FakeApplication(),val identity:Option[Identity]=None) extends WithApplication(app) with Mockito {

  lazy val user = identity getOrElse SocialUserGenerator.socialUser()
  lazy val mockUserService=mock[UserService]

  def cookie=Authenticator.create(user) match {
    case Right(authenticator) => authenticator.toCookie
    case _ => throw new IllegalArgumentException("Your FakeApplication _must_ configure a working AuthenticatorStore")
  }

  override def around[T: AsResult](t: =>T): Result = super.around {
    mockUserService.find(user.identityId) returns Some(user)
    UserService.setService(mockUserService)
    t
  }
}

object WithLoggedUser{
  val excludedPlugins = List( "securesocial.core.DefaultAuthenticatorStore" )
  val includedPlugins = List( "securesocial.testkit.FakeAuthenticatorStore" )
  def minimalApp = FakeApplication(withoutPlugins=excludedPlugins,additionalPlugins = includedPlugins)
}



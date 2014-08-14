import java.lang.reflect.Constructor

import controllers.CustomRoutesService
import play.api.GlobalSettings
import play.api.test.{FakeRequest, WithApplication, FakeApplication, PlaySpecification}
import securesocial.core.RuntimeEnvironment
import securesocial.testkit.AlwaysValidIdentityProvider
import service.{DemoUser, MyEventListener, InMemoryUserService}

class ApplicationScenario  extends PlaySpecification {

  def app = FakeApplication( withGlobal=Some(global(env)))
  "A logged in user can view the index" in new WithApplication(app) {
    //Given
    val creds1 = cookies(route(FakeRequest(POST, "/auth/authenticate/naive").withTextBody("user")).get)
    //When
    val Some(response)=route(FakeRequest(GET, "/").withCookies(creds1.get("id").get))

    //Then
    status(response) must equalTo(OK)
  }

  /** This is application specific and can not be put into test-kit **/
  val env=new AlwaysValidIdentityProvider.RuntimeEnvironment[DemoUser] {
    override lazy val routes = new CustomRoutesService()
    override lazy val userService: InMemoryUserService = new InMemoryUserService()
    override lazy val eventListeners = List(new MyEventListener())
  }

  private def global[A](env:RuntimeEnvironment[A]): GlobalSettings =
    new play.api.GlobalSettings {
      override def getControllerInstance[A](controllerClass: Class[A]): A = {
        val instance = controllerClass.getConstructors.find { c =>
          val params = c.getParameterTypes
          params.length == 1 && params(0) == classOf[RuntimeEnvironment[A]]
        }.map {
          _.asInstanceOf[Constructor[A]].newInstance(env)
        }
        instance.getOrElse(super.getControllerInstance(controllerClass))
      }
    }
}

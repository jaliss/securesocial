package scenarios

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import play.api.test.{FakeApplication, PlaySpecification, WithApplication}

import scenarios.helpers.{TestGlobal, DemoUser, TestUserService}
import securesocial.core.providers.GoogleProvider
import securesocial.core.{IdentityProvider, EventListener, RuntimeEnvironment}
import securesocial.core.services.{HttpService, UserService, RoutesService}
import play.api.libs.ws.WS.WSRequestHolder
import org.specs2.matcher.Matcher
import play.api.libs.ws.Response
import scala.concurrent.Future
import play.api.libs.json.Json

import scenarios.helpers.Api._
import java.io.File

class GoogleLoginSpec extends PlaySpecification with Mockito {


  def hasCode:Matcher[Map[String,Seq[String]]]=(map:Map[String,Seq[String]])=>map must contain("code" -> Seq("code"))
  "an application using secure social" should {
    val testGlobal=new TestGlobal(new TestEnvironment(_httpService = mock[HttpService]))
    val application: FakeApplication = FakeApplication(withGlobal = Some(testGlobal), additionalConfiguration = googleConfig )
    "log a valid google user" in new WithApplication(application){
      val _httpService: HttpService = mock[HttpService].as("_httpService")
      val userInfoHolder: WSRequestHolder = mock[WSRequestHolder].as("userInfoHolder")
      val userInfoResponse: Response = mock[Response].as("userInfoResponse")
      val accessTokenHolder: WSRequestHolder = mock[WSRequestHolder].as("accessTokenHolder")
      val accessTokenResponse: Response = mock[Response].as("accessTokenResponse")
      _httpService.url("accessTokenUrl") returns accessTokenHolder
      _httpService.url("https://www.googleapis.com/oauth2/v1/userinfo?access_token=accessToken") returns userInfoHolder
      accessTokenHolder.post(any[Map[String,Seq[String]]])(any,any) returns Future.successful(accessTokenResponse)
      userInfoHolder.get() returns Future.successful(userInfoResponse)
      accessTokenResponse.json returns Json.parse(
        """
          |{ "access_token": "accessToken" }
        """.stripMargin)
      userInfoResponse.json returns Json.parse(
        """
          |{
          |"id":"identity",
          |"given_name": "john",
          |"name": "john smith",
          |"family_name": "smith",
          |"email": "test@example.com"
          |}
        """.stripMargin)
      testGlobal.runtimeEnvironment=new TestEnvironment(_httpService = _httpService)

      val loginResponse = authenticate(GoogleProvider.Google)
      status(loginResponse) === 303
      val location: Option[String] = redirectLocation(loginResponse)
      location must beSome(startWith("authorizationUrl"))
      val state = "state=([^&]*)".r.findFirstMatchIn(location.get).get.group(1)
      val sid=session(loginResponse).get("sid").get

      val authenticateResponse = callback(GoogleProvider.Google,Some(state), Some("code"), session=List(("sid",sid)))
      status(authenticateResponse) === 303
      redirectLocation(authenticateResponse)=== Some("/")
    }
  }
  
  import scala.collection.immutable.ListMap

  class TestEnvironment( _routes: => RoutesService = new RoutesService.Default(),
                         _userService: =>UserService[DemoUser] = new TestUserService(),
                         _eventListeners: => List[EventListener[DemoUser]] = Nil,
                         _httpService : => HttpService = new HttpService.Default()
                         ) extends RuntimeEnvironment.Default[DemoUser] {
    override lazy val routes: RoutesService = _routes
    override lazy val userService: UserService[DemoUser] = _userService
    override lazy val eventListeners: List[EventListener[DemoUser]] = _eventListeners
    override lazy val httpService : HttpService = _httpService
    override lazy val providers: ListMap[String, IdentityProvider] = ListMap(include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))))
  }
  val googleConfig = Map(
     "smtp.mock"->true
    , "application.secret"->"secret"
    , "application.router"->"securesocial.Routes"
    , "securesocial.google.clientId"->"clientid"
    , "securesocial.google.clientSecret"->"clientsecret"
    , "securesocial.google.authorizationUrl"->"authorizationUrl"
    , "securesocial.google.accessTokenUrl"->"accessTokenUrl"
    , "securesocial.google.scope"->"scope"
  )

}

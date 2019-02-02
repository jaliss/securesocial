package securesocial.core

import helpers.MockHttpService
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{ JsObject, Json }
import play.api.libs.ws.WSResponse
import securesocial.core.services.HttpService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuth2ClientSpec extends Specification with Mockito {
  "The default OAuth2Client" should {
    import MockHttpService._
    "exchange a signin code for a token" in {
      implicit ee: ExecutionEnv =>
        val httpService: MockHttpService = new MockHttpService()
        val client = aDefaultClient(httpService)
        val (code, callbackUrl) = ("code", "callbackUrl")
        val expectedJson: JsObject = Json.obj("expected" -> "object")
        val expectedToken = OAuth2Info("accessToken")

        httpService.request.post(any[Params])(any[ParamsWriter]) returns Future.successful(httpService.response)
        httpService.response.json returns expectedJson

        val builder: WSResponse => OAuth2Info = (response: WSResponse) => if (response.json == expectedJson) expectedToken else throw new RuntimeException(s"Expected ${response.json} to be $expectedJson")
        val token: Future[OAuth2Info] = client.exchangeCodeForToken(code, callbackUrl, builder)

        token must beEqualTo(expectedToken).await
    }
    "retrieve the profile given an access token" in {
      implicit ee: ExecutionEnv =>
        val httpService: MockHttpService = new MockHttpService()
        val client = aDefaultClient(httpService)
        val profileUrl = "profileUrl"
        val expectedJson: JsObject = Json.obj("expected" -> "object")
        httpService.response.json returns expectedJson

        val actualProfile = client.retrieveProfile(profileUrl)

        actualProfile must beEqualTo(expectedJson).await
    }

  }
  private def aDefaultClient(httpService: HttpService = new MockHttpService()) = {
    new OAuth2Client.Default(httpService, fakeOAuth2Settings) {
    }
  }
  val fakeOAuth2Settings = new OAuth2Settings(
    "authorizationUrl", "accessTokenUrl", "clientId", "clientSecret", Some("scope"), Map.empty, Map.empty)
}

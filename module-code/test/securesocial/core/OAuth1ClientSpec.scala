package securesocial.core

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import helpers.MockHttpService
import play.api.libs.oauth._
import oauth.signpost.exception.OAuthException
import play.api.libs.json.Json
import securesocial.core.services.HttpService
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.OAuth
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.ConsumerKey

class OAuth1ClientSpec extends Specification with Mockito {
  val fakeServiceInfo= new ServiceInfo("requestTokenUrl","accessTokenUrl", "authorizationUrl", ConsumerKey("consumerKey","consumerSecret"))

  "The default OAuth1Client" should {
    "provide the redirectUrl given a token" in {
      val client=aDefaultClient()
      val token: String = "token"
      val expectedRedirectUrl: String = "redirectUrl"
      client.client.redirectUrl(token) returns expectedRedirectUrl

      val actualRedirectUrl: String = client.redirectUrl(token)
      actualRedirectUrl === expectedRedirectUrl
    }
    "retrieve the requestToken when the endpoint is correct" in {
      implicit val ec = helpers.sequentialExecutionContext
      val client=aDefaultClient()
      val callbackUrl: String = "callbackUrl"
      val expectedRequestToken = RequestToken("token","secret")
      client.client.retrieveRequestToken(callbackUrl) returns Right(expectedRequestToken)

      val actualRequestToken = client.retrieveRequestToken(callbackUrl)

      actualRequestToken must beEqualTo(expectedRequestToken).await
    }
    "fail to retrieve the requestToken with an OAuthException when the endpoint is incorrect" in {
      implicit val ec = helpers.sequentialExecutionContext
      val client=aDefaultClient()
      val callbackUrl: String = "incorrectCallbackUrl"
      val expectedException= new OAuthException("invalid endpoint") {}
      client.client.retrieveRequestToken(callbackUrl) returns Left(expectedException)

      val actualRequestToken = client.retrieveRequestToken(callbackUrl)

      actualRequestToken must throwAn(expectedException).await
    }
    "retrieve the OAuth1Info given a valid request token and a verifier" in {
      implicit val ec = helpers.sequentialExecutionContext
      val client=aDefaultClient()
      val verifier: String = "verifier"
      val requestToken = RequestToken("requestToken","requestSecret")
      val accessToken: RequestToken = RequestToken("accessToken", "accessSecret")

      client.client.retrieveAccessToken(requestToken,verifier) returns Right(accessToken)

      val actualOAuth1Info= client.retrieveOAuth1Info(requestToken,verifier)

      actualOAuth1Info must beEqualTo(OAuth1Info(accessToken.token,accessToken.secret)).await
    }
    "fail to retrieve the OAuth1Info given an invalid request token and a verifier" in {
      implicit val ec = helpers.sequentialExecutionContext
      val client=aDefaultClient()
      val verifier: String = "verifier"
      val requestToken = RequestToken("invalidRequestToken","requestSecret")
      val expectedException= new OAuthException("invalid endpoint") {}

      client.client.retrieveAccessToken(requestToken,verifier) returns Left(expectedException)

      val actualOAuth1Info= client.retrieveOAuth1Info(requestToken,verifier)

      actualOAuth1Info must throwAn(expectedException).await
    }
    "retrieve the json profile given the profile api url and the OAuth1Info" in {
      implicit val ec = helpers.sequentialExecutionContext
      val httpService: MockHttpService = new MockHttpService()
      val client=aDefaultClient(httpService)
      val profileApiUrl: String = "profileApiUrl"
      val oauth1info = OAuth1Info("accessToken","accessSecret")
      val expectedJsonProfile = Json.obj("id"->"success")

      httpService.request.sign(any[OAuthCalculator]) returns httpService.request //make sure the request is signed
      httpService.response.json returns expectedJsonProfile

      val actualJsonProfile= client.retrieveProfile(profileApiUrl, oauth1info)

      actualJsonProfile must beEqualTo(expectedJsonProfile).await
    }
  }

  private def aDefaultClient(httpService:HttpService = new MockHttpService())={
    new OAuth1Client.Default(fakeServiceInfo, httpService){
      override val client = mock[OAuth]
    }
  }
}

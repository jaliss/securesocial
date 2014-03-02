import play.api.test.{FakeRequest, WithApplication, FakeApplication, PlaySpecification}

class ApplicationScenario  extends PlaySpecification {
  def app = FakeApplication(additionalPlugins = Seq("securesocial.testkit.AlwaysValidIdentityProvider"))
  "A logged in user can view the index" in new WithApplication(app) {
    //Given
    val creds1 = cookies(route(FakeRequest(POST, "/authenticate/naive").withTextBody("user")).get)
    //When
    val Some(response)=route(FakeRequest(GET, "/").withCookies(creds1.get("id").get))

    //Then
    status(response) must equalTo(OK)
  }
}

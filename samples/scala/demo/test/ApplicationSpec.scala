import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "render the signup page in English" in new WithApplication{
      testSignUpLocalization("en", "Sign Up")
    }

    "render the signup page in Dutch" in new WithApplication{
      testSignUpLocalization("nl", "Aanmelden")
    }

  }
  
  private def testSignUpLocalization(lang: String, expected: String) = {
      val home = route(FakeRequest(GET, "/signup").withHeaders(ACCEPT_LANGUAGE -> lang)).get
      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain (expected)
  }
}

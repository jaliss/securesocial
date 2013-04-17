/**
 * Copyright 2013 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.api.test.{TestBrowser, FakeApplication, TestServer}
import securesocial.core.Authenticator

/**
 * Base class for SecureSocial tests
 */
class BaseSpec extends Specification {
  val testConfiguration = ConfigFactory.load("tests")
  val appUrl = testConfiguration.getString("tests.serverUrl")
  val testServerPort = testConfiguration.getInt("tests.serverPort")

  /**
   * creates a test server
   *
   * @return
   */
  def testServer = TestServer(testServerPort)

  /**
   * creates a test server with a fake app
   *
   * @param app a fake application
   * @return
   */
  def testServer(app: FakeApplication) = TestServer(testServerPort, app)

  /**
   * logs out the browser session
   *
   * @param browser
   * @return
   */
  def logout(browser: TestBrowser) = {
    browser.goTo(appUrl + "/logout")
    thereMustNotBeAnAuthenticator(browser)
  }

  /**
   * Returns the authenticator cookie
   *
   * @param browser the browser instance being used in the test
   * @return an optional Cookie instance
   */
  def authenticatorCookie(browser: TestBrowser): Option[org.openqa.selenium.Cookie] = {
    import scala.collection.JavaConverters._
    browser.getCookies().asScala.find(_.getName == Authenticator.cookieName)
  }

  /**
   * Asserts the browser session has an authenticator cookie
   *
   * @param browser the browser instance being used in the test
   * @return
   */
  def thereMustBeAnAuthenticator(browser: TestBrowser) = {
    authenticatorCookie(browser) must not beNone
  }

  /**
   * Asserts the browser session must not have an authenticator cookie
   * @param browser
   * @return
   */
  def thereMustNotBeAnAuthenticator(browser: TestBrowser) = {
    authenticatorCookie(browser) must beNone
  }
}

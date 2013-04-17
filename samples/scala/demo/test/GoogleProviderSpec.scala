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
import play.api.test.Helpers._

class GoogleProviderSpec extends BaseSpec {

  val googleAccount = testConfiguration.getString("tests.google.email")
  val googleAccountPassword = testConfiguration.getString("tests.google.password")

  "A user" should {
    "be able to log in using Google and logout" in {
      running(testServer, HTMLUNIT) { browser =>
        browser.goTo(appUrl)
        thereMustNotBeAnAuthenticator(browser)

        val link = browser.$("a[href=\"/authenticate/google\"]")
        link.click()
        browser.fill("#Email").`with`(googleAccount)
        browser.fill("#Passwd").`with`(googleAccountPassword)
        browser.submit("#signIn")

        val buttonId = "#submit_approve_access"
        val button = browser.$(buttonId)
        if( !button.isEmpty ) {
          // adding some sleep time to wait for the
          // approve button to become clickable
          Thread.sleep(10000)

          // click fails with the current fluentlenium/selenium version
          // so browser.submit is used instead.
          //button.click()

          browser.submit(buttonId)

        }
        browser.$("title").getTexts.get(0) must contain("Sample Protected Page")
        thereMustBeAnAuthenticator(browser)

        logout(browser)
      }
    }
  }
}

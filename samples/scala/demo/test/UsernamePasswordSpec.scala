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
import play.api.test._
import play.api.test.Helpers._
import securesocial.test.helpers.MockMailer


/**
 * Tests for the UsernamePassword provider
 */
class UsernamePasswordSpec extends BaseSpec {

  val userEmail = "john.doe@example.com"
  val userName = "John"
  val userLastName = "Doe"
  val userPassword = "my_secret_password"
  val newPassword = "my_new_password"

  "A user" should {
    "be redirected to the login page if he is not authenticated" in {
      running(testServer, HTMLUNIT) { browser =>
        browser.goTo(appUrl)
        browser.$("title").getTexts().get(0) must beEqualTo("Login")
      }
    }

    "be able to sign up, log in, change his password and logout" in {
      running(testServer(FakeApplication(
        additionalPlugins = Seq("securesocial.test.helpers.MockMailPlugin"),
        withoutPlugins = Seq("com.typesafe.plugin.CommonsMailerPlugin")
      )), HTMLUNIT) { browser =>
      // go to the sign up page and fill the email
        browser.goTo(appUrl + "/signup")
        browser.fill("#email").`with`(userEmail)
        browser.find("form").submit()

        browser.pageSource() must contain("check your email")

        // check the email and get the next page
        MockMailer.waitForEmail()
        val confirmPage = MockMailer.bodyHtml.map( html => {
          val from = html.indexOf("href=") + 6
          Some(html.substring(from, html.indexOf(">link") - 1))
        }).getOrElse(None)

        confirmPage.isDefined must beTrue

        // complete registration form
        browser.goTo(confirmPage.get)
        browser.fill("#firstName").`with`(userName)
        browser.fill("#lastName").`with`(userLastName)
        browser.fill("#password_password1").`with`(userPassword)
        browser.fill("#password_password2").`with`(userPassword)
        browser.find("form").submit()

        // confirm the form was processed correctly
        browser.pageSource() must contain ("Thank you for signing up")

        // log in using the new account
        browser.fill("#username").`with`(userEmail)
        browser.fill("#password").`with`(userPassword)
        browser.find("form").submit()
        browser.pageSource() must contain("Welcome")

        // confirm we have a session cookie
        thereMustBeAnAuthenticator(browser)

        // change the password
        browser.$("a[href=\"%s/password\"]".format(appUrl)).click()
        browser.fill("#currentPassword").`with`(userPassword)
        browser.fill("#newPassword_password1`").`with`(newPassword)
        browser.fill("#newPassword_password2`").`with`(newPassword)
        browser.find("form").submit()
        browser.pageSource() must contain("Your password was changed")

        // logout and log in using new password
        logout(browser)
        browser.fill("#username").`with`(userEmail)
        browser.fill("#password").`with`(newPassword)
        browser.find("form").submit()
        browser.pageSource() must contain("Welcome")
        thereMustBeAnAuthenticator(browser)

        //
        logout(browser)
      }
    }
  }
}


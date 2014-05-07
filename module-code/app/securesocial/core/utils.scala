/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core

import play.api.mvc.{Cookies, Session, SimpleResult}
import play.api.http.HeaderNames
import securesocial.core.authenticator.Authenticator

/**
 * Utility methods
 */
object utils {

  /**
   * Helper methods for SimpleResult
   * @param r a SimpleResult instance
   */
  implicit class SimpleResultMethods(val r: SimpleResult) {
    def startingAuthenticator[A](authenticator: Authenticator[A]) = authenticator.starting(r)
    def discardingAuthenticator[A](authenticator: Authenticator[A]) = authenticator.discarding(r)
    def touchingAuthenticator[A](authenticator: Authenticator[A]) = authenticator.touching(r)
    def addToSession(values: (String, String)*) = {
      val cookies = Cookies(r.header.headers.get(HeaderNames.SET_COOKIE))
      val resultSession = Session.decodeFromCookie(cookies.get(Session.COOKIE_NAME))
      def addValues(list: List[(String, String)], session: Session): Session = {
        if ( list.isEmpty) session else {
          val s = session + list.head
          addValues(list.tail, s)
        }
      }
      r.withSession(addValues(values.toList, resultSession))
    }
  }
}
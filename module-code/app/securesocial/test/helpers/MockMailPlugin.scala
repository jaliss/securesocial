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
package securesocial.test.helpers

import com.typesafe.plugin.{MailerAPI, MailerPlugin, MailerBuilder}

/**
 * A plugin to handle emails during tests. It allows the test to wait for emails
 * sent by SecureSocial.
 */
case object MockMailer extends MailerBuilder {

  val BodyText = "bodyText"
  val BodyHtml = "bodyHtml"

  protected val messageHolder = collection.mutable.Map[String,String]()

  private val monitor = new Object()

  def waitForEmail() {
    monitor.synchronized {
      monitor.wait(5000)
    }
  }

  def clear() {
    messageHolder.clear()
  }

  def setBodyText(s: String) {
    messageHolder.put(BodyText, s)
  }

  def setBodyHtml(s: String) {
    messageHolder.put(BodyHtml, s)
  }

  def bodyHtml: Option[String] = messageHolder.get(BodyHtml)

  def send(bodyText: String, bodyHtml: String) {
    if ( !bodyHtml.isEmpty ) setBodyHtml( bodyHtml )
    if ( !bodyText.isEmpty ) setBodyText( bodyText )
    monitor.synchronized( monitor.notifyAll() )
  }
}

case class MockMailPlugin(app: play.api.Application) extends MailerPlugin {
  def email: MailerAPI = MockMailer
}

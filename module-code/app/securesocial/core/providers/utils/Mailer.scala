/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core.providers.utils

import securesocial.core.Identity
import play.api.{Play, Logger}
import securesocial.controllers.TemplatesPlugin
import com.typesafe.plugin._
import Play.current
import play.api.libs.concurrent.Akka
import play.api.mvc.RequestHeader
import play.api.i18n.Messages
import play.api.templates.{Html, Txt}

/**
 * A helper class to send email notifications
 */
object Mailer {
  val fromAddress = current.configuration.getString("smtp.from").get
  val AlreadyRegisteredSubject = "mails.sendAlreadyRegisteredEmail.subject"
  val SignUpEmailSubject = "mails.sendSignUpEmail.subject"
  val WelcomeEmailSubject = "mails.welcomeEmail.subject"
  val PasswordResetSubject = "mails.passwordResetEmail.subject"
  val UnknownEmailNoticeSubject = "mails.unknownEmail.subject"
  val PasswordResetOkSubject = "mails.passwordResetOk.subject"


  def sendAlreadyRegisteredEmail(user: Identity)(implicit request: RequestHeader) {
    val txtAndHtml = use[TemplatesPlugin].getAlreadyRegisteredEmail(user)
    sendEmail(Messages(AlreadyRegisteredSubject), user.email.get, txtAndHtml)

  }

  def sendSignUpEmail(to: String, token: String)(implicit request: RequestHeader)  {
    val txtAndHtml = use[TemplatesPlugin].getSignUpEmail(token)
    sendEmail(Messages(SignUpEmailSubject), to, txtAndHtml)
  }

  def sendWelcomeEmail(user: Identity)(implicit request: RequestHeader) {
    val txtAndHtml = use[TemplatesPlugin].getWelcomeEmail(user)
    sendEmail(Messages(WelcomeEmailSubject), user.email.get, txtAndHtml)

  }

  def sendPasswordResetEmail(user: Identity, token: String)(implicit request: RequestHeader) {
    val txtAndHtml = use[TemplatesPlugin].getSendPasswordResetEmail(user, token)
    sendEmail(Messages(PasswordResetSubject), user.email.get, txtAndHtml)
  }

  def sendUnkownEmailNotice(email: String)(implicit request: RequestHeader) {
    val txtAndHtml = use[TemplatesPlugin].getUnknownEmailNotice()
    sendEmail(Messages(UnknownEmailNoticeSubject), email, txtAndHtml)
  }

  def sendPasswordChangedNotice(user: Identity)(implicit request: RequestHeader) {
    val txtAndHtml = use[TemplatesPlugin].getPasswordChangedNoticeEmail(user)
    sendEmail(Messages(PasswordResetOkSubject), user.email.get, txtAndHtml)
  }

  private def sendEmail(subject: String, recipient: String, body: (Option[Txt], Option[Html])) {
    import com.typesafe.plugin._
    import scala.concurrent.duration._
    import play.api.libs.concurrent.Execution.Implicits._

    if ( Logger.isDebugEnabled ) {
      Logger.debug("[securesocial] sending email to %s".format(recipient))
      Logger.debug("[securesocial] mail = [%s]".format(body))
    }

    Akka.system.scheduler.scheduleOnce(1 seconds) {
      val mail = use[MailerPlugin].email
      mail.setSubject(subject)
      mail.setRecipient(recipient)
      mail.setFrom(fromAddress)
      // the mailer plugin handles null / empty string gracefully
      mail.send(body._1.map(_.body).getOrElse(""), body._2.map(_.body).getOrElse(""))
    }
  }
}

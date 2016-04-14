/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
  * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.core.providers.utils

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.Application
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.i18n.Messages
import play.twirl.api.{Html, Txt}
import securesocial.controllers.MailTemplates
import securesocial.core.BasicProfile

/**
 * A helper trait to send email notifications
 */
trait Mailer {
  def sendAlreadyRegisteredEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang)

  def sendSignUpEmail(to: String, token: String)(implicit request: RequestHeader, lang: Lang)

  def sendWelcomeEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang)

  def sendPasswordResetEmail(user: BasicProfile, token: String)(implicit request: RequestHeader, lang: Lang)

  def sendUnkownEmailNotice(email: String)(implicit request: RequestHeader, lang: Lang)

  def sendPasswordChangedNotice(user: BasicProfile)(implicit request: RequestHeader, lang: Lang)

  def sendEmail(subject: String, recipient: String, body: (Option[Txt], Option[Html]))
}

object Mailer {

  import play.api.libs.mailer._

  @Inject
  implicit var current: Application = null
  @Inject
  implicit var messages: Messages = null

  /**
   * The default mailer implementation
   *
   * @param mailTemplates the mail templates
   */
  class Default(mailTemplates: MailTemplates) extends Mailer {
    private val logger = play.api.Logger("securesocial.core.providers.utils.Mailer.Default")
    val fromAddress = current.configuration.getString("play.mailer.from").get
    val AlreadyRegisteredSubject = "mails.sendAlreadyRegisteredEmail.subject"
    val SignUpEmailSubject = "mails.sendSignUpEmail.subject"
    val WelcomeEmailSubject = "mails.welcomeEmail.subject"
    val PasswordResetSubject = "mails.passwordResetEmail.subject"
    val UnknownEmailNoticeSubject = "mails.unknownEmail.subject"
    val PasswordResetOkSubject = "mails.passwordResetOk.subject"

    override def sendAlreadyRegisteredEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang) {
      val txtAndHtml = mailTemplates.getAlreadyRegisteredEmail(user)
      sendEmail(messages.at(AlreadyRegisteredSubject), user.email.get, txtAndHtml)

    }

    override def sendSignUpEmail(to: String, token: String)(implicit request: RequestHeader, lang: Lang) {
      val txtAndHtml = mailTemplates.getSignUpEmail(token)
      sendEmail(messages.at(SignUpEmailSubject), to, txtAndHtml)
    }

    override def sendWelcomeEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang) {
      val txtAndHtml = mailTemplates.getWelcomeEmail(user)
      sendEmail(messages.at(WelcomeEmailSubject), user.email.get, txtAndHtml)

    }

    override def sendPasswordResetEmail(user: BasicProfile, token: String)(implicit request: RequestHeader, lang: Lang) {
      val txtAndHtml = mailTemplates.getSendPasswordResetEmail(user, token)
      sendEmail(messages.at(PasswordResetSubject), user.email.get, txtAndHtml)
    }

    override def sendUnkownEmailNotice(email: String)(implicit request: RequestHeader, lang: Lang) {
      val txtAndHtml = mailTemplates.getUnknownEmailNotice()
      sendEmail(messages.at(UnknownEmailNoticeSubject), email, txtAndHtml)
    }

    override def sendPasswordChangedNotice(user: BasicProfile)(implicit request: RequestHeader, lang: Lang) {
      val txtAndHtml = mailTemplates.getPasswordChangedNoticeEmail(user)
      sendEmail(messages.at(PasswordResetOkSubject), user.email.get, txtAndHtml)
    }

    override def sendEmail(subject: String, recipient: String, body: (Option[Txt], Option[Html])) {
      import play.api.libs.concurrent.Execution.Implicits._

      import scala.concurrent.duration._

      logger.debug(s"[securesocial] sending email to $recipient")
      logger.debug(s"[securesocial] mail = [$body]")

      @Inject
      implicit var actorSystem: ActorSystem = null
      @Inject
      implicit var MailerPlugin: MailerClient = null

      actorSystem.scheduler.scheduleOnce(1.seconds) {
        val mail = Email(subject, fromAddress, Seq(recipient), body._1.map(txt => txt.body), body._2.map(html => html.body))
        MailerPlugin.send(mail)
      }
    }
  }

}
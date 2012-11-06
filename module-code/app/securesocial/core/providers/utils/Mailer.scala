package securesocial.core.providers.utils

import securesocial.core.SocialUser
import play.api.{Play, Logger}
import securesocial.controllers.TemplatesPlugin
import com.typesafe.plugin._
import Play.current
import play.api.libs.concurrent.Akka
import play.api.mvc.RequestHeader
import play.api.i18n.Messages

/**
 * todo: externalize subjects
 */
object Mailer {
  val fromAddress = current.configuration.getString("smtp.from").get
  val AlreadyRegisteredSubject = "mails.sendAlreadyRegisteredEmail.subject"
  val SignUpEmailSubject = "mails.sendSignUpEmail.subject"
  val WelcomeEmailSubject = "mails.welcomeEmail.subject"
  val PasswordResetSubject = "mails.passwordResetEmail.subject"
  val UnknownEmailNoticeSubject = "mails.unknownEmail.subject"
  val PasswordResetOkSubject = "mails.passwordResetOk.subject"


  def sendAlreadyRegisteredEmail(user: SocialUser)(implicit request: RequestHeader) {
    val html = use[TemplatesPlugin].getAlreadyRegisteredEmail(user)
    sendEmail(Messages(AlreadyRegisteredSubject), user.email.get, html)

  }

  def sendSignUpEmail(to: String, token: String)(implicit request: RequestHeader)  {
    val html = use[TemplatesPlugin].getSignUpEmail(token)
    sendEmail(Messages(SignUpEmailSubject), to, html)
  }

  def sendWelcomeEmail(user: SocialUser)(implicit request: RequestHeader) {
    val html = use[TemplatesPlugin].getWelcomeEmail(user)
    sendEmail(Messages(WelcomeEmailSubject), user.email.get, html)

  }

  def sendPasswordResetEmail(user: SocialUser, token: String)(implicit request: RequestHeader) {
    val html = use[TemplatesPlugin].getSendPasswordResetEmail(user, token)
    sendEmail(Messages(PasswordResetSubject), user.email.get, html)
  }

  def sendUnkownEmailNotice(email: String)(implicit request: RequestHeader) {
    val html = use[TemplatesPlugin].getUnknownEmailNotice()
    sendEmail(Messages(UnknownEmailNoticeSubject), email, html)
  }

  def sendPasswordChangedNotice(user: SocialUser)(implicit request: RequestHeader) {
    val html = use[TemplatesPlugin].getPasswordChangedNotice(user)
    sendEmail(Messages(PasswordResetOkSubject), user.email.get, html)
  }

  private def sendEmail(subject: String, recipient: String, body: String) {
    import com.typesafe.plugin._
    import akka.util.duration._

    if ( Logger.isDebugEnabled ) {
      Logger.debug("Sending email to %s".format(recipient))
      Logger.debug("mail = [%s]".format(body))
    }

    Akka.system.scheduler.scheduleOnce(1 seconds) {
      val mail = use[MailerPlugin].email
      mail.setSubject(subject)
      mail.addRecipient(recipient)
      mail.addFrom(fromAddress)
      mail.sendHtml(body)
    }
  }
}

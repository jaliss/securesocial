package securesocial.controllers.registration

import play.api.mvc.{ Result, Action, Controller }
import play.api.mvc.Results._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.{ Play, Logger }
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core._
import com.typesafe.plugin._
import Play.current
import securesocial.core.providers.utils._
import org.joda.time.DateTime
import play.api.i18n.Messages
import securesocial.core.providers.Token
import scala.Some
import securesocial.core.IdentityId
import securesocial.controllers.TemplatesPlugin
import securesocial.controllers.ProviderController
import securesocial.controllers.ProviderController.landingUrl

object FullRegistration extends Controller with securesocial.core.SecureSocial {
  import DefaultRegistration.{
    RegistrationInfo,
    UserName,
    UserNameAlreadyTaken,
    providerId,
    FirstName,
    LastName,
    NickName,
    Password,
    Password1,
    Password2,
    PasswordsDoNotMatch,
    Email,
    Success,
    SignUpDone,
    onHandleStartSignUpGoTo,
    ThankYouCheckEmail,
    TokenDurationKey,
    DefaultDuration,
    TokenDuration,
    createToken,
    executeForToken
  }

  val NotActive = "NotActive"
  val EmailAlreadyTaken = "securesocial.signup.emailAlreadyTaken"

  case class FullRegistrationInfo(userName: Option[String], firstName: Option[String], lastName: Option[String], nickName: String, email: String, password: String)

  val formWithUsername = Form[FullRegistrationInfo](
    mapping(
      UserName -> nonEmptyText.verifying(Messages(UserNameAlreadyTaken), userName => {
        UserService.find(IdentityId(userName, providerId)).isEmpty
      }),
      FirstName -> optional(text),
      LastName -> optional(text),
      NickName -> nonEmptyText,
      Email -> email.verifying(nonEmpty),
      (Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(use[PasswordValidator].errorMessage,
            p => use[PasswordValidator].isValid(p)),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2))) // binding
          ((userName, firstName, lastName, nickName, email, password) => FullRegistrationInfo(Some(userName), firstName, lastName, nickName, email, password._1)) // unbinding
          (info => Some(info.userName.getOrElse(""), info.firstName, info.lastName, info.nickName, info.email, ("", ""))))

  val formWithoutUsername = Form[FullRegistrationInfo](
    mapping(
      FirstName -> optional(text),
      LastName -> optional(text),
      NickName -> nonEmptyText,
      UserName -> email.verifying(nonEmpty),
      (Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(use[PasswordValidator].errorMessage,
            p => use[PasswordValidator].isValid(p)),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2))) // binding
          ((firstName, lastName, nickName, email, password) => FullRegistrationInfo(None, firstName, lastName, nickName, email, password._1)) // unbinding
          (info => Some(info.firstName, info.lastName, info.nickName, info.email, ("", ""))))

  val form = if (UsernamePasswordProvider.withUserNameSupport) formWithUsername else formWithoutUsername

  def signUp = Action { implicit request =>
    if (Logger.isDebugEnabled) {
      Logger.debug("[securesocial] trying sign up")
    }
    Ok(use[TemplatesPlugin].getFullSignUpPage(request, form))
  }

  /**
   * Handles posts from the sign up page
   */

  def handleSignUp = Action { implicit request =>
    form.bindFromRequest.fold(
      errors => {
        if (Logger.isDebugEnabled) {
          Logger.debug("[securesocial] errors " + errors)
        }
        BadRequest(use[TemplatesPlugin].getFullSignUpPage(request, errors))
      },
      info => {
        UserService.findByEmailAndProvider(info.email, providerId) match {
          case None =>
            val id = info.email
            val user = SocialUser(
              IdentityId(id, providerId),
              info.firstName getOrElse "",
              info.lastName getOrElse "",
              "%s %s".format(info.firstName, info.lastName),
              Some(info.nickName),
              NotActive,
              Some(info.email),
              GravatarHelper.avatarFor(info.email),
              AuthenticationMethod.UserPassword,
              passwordInfo = Some(Registry.hashers.currentHasher.hash(info.password)))
            UserService.save(user)
            Events.fire(new SignUpEvent(user)).getOrElse(session)
            val token = createToken(info.email, isSignUp = true)
            Mailer.sendVerificationEmail(info.email, token._1)
          case Some(alreadyRegisteredUser) =>
            Mailer.sendAlreadyRegisteredEmail(alreadyRegisteredUser)
        }
        Redirect(onHandleStartSignUpGoTo).flashing(Success -> Messages(ThankYouCheckEmail), Email -> info.email)
      })
  }

  def signUpVerification(token: String) = UserAwareAction { implicit request =>
    def markAsActive(user: Identity) {
      val updated = UserService.save(SocialUser(user).copy(state = "Active"))
      Mailer.sendWelcomeEmail(updated)
      val eventSession = Events.fire(new SignUpEvent(updated)).getOrElse(session)
      ProviderController.completeAuthentication(updated, eventSession).flashing(Success -> Messages(SignUpDone))
    }
    executeForToken(token, true, { t =>
      val email = t.email
      val providerId = t.uuid
      val userFromToken = UserService.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword)
      (userFromToken, request.user) match {
        case (Some(user), Some(user2)) if user.identityId == user2.identityId =>
          markAsActive(user)
          Redirect(landingUrl)
        case (Some(user), None) =>
          markAsActive(user)
          Redirect(RoutesHelper.login().url)
        case _ =>
          Unauthorized("Not Authorized Page")
      }
    })
  }
}

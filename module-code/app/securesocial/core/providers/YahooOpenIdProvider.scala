package securesocial.core.providers

import securesocial.core.IdentityProvider
import play.api.Application
import securesocial.core.AuthenticationMethod
import play.api.mvc.Result
import securesocial.core.SocialUser
import play.api.mvc.Request
import play.api.libs.openid.OpenID
import securesocial.core.providers.utils.RoutesHelper
import play.api.mvc.AsyncResult
import play.api.mvc.Results.Redirect
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.AsyncResult
import scala.collection.JavaConversions._
import play.api.libs.openid.UserInfo
import securesocial.core.UserId
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

class YahooOpenIdProvider(app: play.api.Application) extends IdentityProvider(app) {

  override def authMethod = AuthenticationMethod.OpenId

  override def doAuth[A]()(implicit request: Request[A]): Either[Result, SocialUser] = {
    if (!request.queryString.get("openid.mode").isDefined) {
      val callbackUrl = RoutesHelper.authenticate(id).absoluteURL(IdentityProvider.sslEnabled)
      val redirectUrl = OpenID.redirectURL("https://me.yahoo.com", callbackUrl, axRequired = Seq("email" -> "http://axschema.org/contact/email"))
      Left(AsyncResult(redirectUrl.map(url => Redirect(url))))
    } else {
      Right(extract(Await.result(OpenID.verifiedId, 10 seconds)))
    }
  }

  private def extract(info: UserInfo): SocialUser = {
    val id = info.id
    SocialUser(UserId(id, id), "", "",
        info.attributes.get("fullname").getOrElse(""), info.attributes.get("email"), None,
        AuthenticationMethod.OpenId, None, None, None)
  }

  override def fillProfile(user: SocialUser): SocialUser = {
    user
  }

  override def id = "yahoo"

}
package securesocial.testkit

import securesocial.core.{Authenticator, AuthenticatorStore}
import play.api.Application

class FakeAuthenticatorStore(app:Application) extends AuthenticatorStore(app) {
  var authenticator:Option[Authenticator] = None
  def save(authenticator: Authenticator): Either[Error, Unit] = {
    this.authenticator=Some(authenticator)
    Right()
  }
  def find(id: String): Either[Error, Option[Authenticator]] = {
    Some(authenticator.filter(_.id == id)).toRight(new Error("no such authenticator"))
  }
  def delete(id: String): Either[Error, Unit] = {
    this.authenticator=None
    Right()
  }
}
package securesocial.testkit

import org.joda.time.DateTime
import play.api.mvc.SimpleResult
import play.mvc.Http.Context
import securesocial.core.authenticator.Authenticator

import scala.concurrent.Future

case class FakeAuthenticator[A](id:String, user:A, creationDate: DateTime , expirationDate: DateTime , lastUsed: DateTime) extends Authenticator[A] {

  override def touch: Future[Authenticator[A]] = Future.successful(copy(lastUsed=DateTime.now))

  override def updateUser(user: A): Future[Authenticator[A]] = Future.successful(copy(user=user))

  override def discarding(result: SimpleResult): Future[SimpleResult] = Future.successful(result)

  override def discarding(javaContext: Context): Future[Unit] = Future.successful(())

  override def isValid: Boolean = true

  override def touching(result: SimpleResult): Future[SimpleResult] = Future.successful(result)

  override def touching(javaContext: Context): Future[Unit] = Future.successful(())

  override def starting(result: SimpleResult): Future[SimpleResult] = Future.successful(result)

}

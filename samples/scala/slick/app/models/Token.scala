package models

import play.api.db.slick.Config.driver.simple._
import securesocial.core.providers.Token
import org.joda.time.DateTime
import slick._

class TokenTable(tag: Tag) extends Table[Token](tag, "usertokens") {

  def uuid = column[String]("uuid", O.PrimaryKey)
  def email = column[String]("email")
  def creationTime = column[DateTime]("creationTime")
  def expirationTime = column[DateTime]("expirationTime")
  def isSignup = column[Boolean]("isSignup")

  def * = (
    uuid,
    email,
    creationTime,
    expirationTime,
    isSignup
  ) <> (Token.tupled, Token.unapply)
}

object Tokens extends TableQuery(new TokenTable(_)) {

  val queryByUUID = Compiled((uuid: Column[String]) => {
    Tokens.filter(_.uuid === uuid)
  })

  def findByUUID(uuid: String)(implicit session: Session): Option[Token] = {
    queryByUUID(uuid).firstOption
  }

  def save(t: Token)(implicit session: Session): Token = {
    findByUUID(t.uuid) match {
      case Some(token) => queryByUUID(token.uuid).update(t)
      case None => Tokens += t
    }
    t
  }

  def deleteExpired()(implicit session: Session) {
    Tokens.filter(t => t.expirationTime <= DateTime.now).delete
  }
}
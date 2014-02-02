package services

import play.api._
import models._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import securesocial.core._
import securesocial.core.providers.Token

import play.api.Play.current

class UserService(application: Application) extends UserServicePlugin(application) {

  def find(id: IdentityId): Option[Identity] = DB.withSession { implicit session =>
    Users.findByIdentityId(id)
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = DB.withSession { implicit session =>
    Users.findByEmailAndProviderId(email, providerId)
  }

  def save(user: Identity): Identity = DB.withTransaction { implicit session =>
    Users.save(user)
  }

  def link(current: Identity, to: Identity) {
    // Implement if you need to link multiple identities
  }

  def save(token: Token): Unit = DB.withTransaction { implicit session =>
    Tokens.save(token)
  }

  def findToken(uuid: String): Option[Token] = DB.withSession { implicit session =>
    Tokens.findByUUID(uuid)
  }

  def deleteToken(uuid: String): Unit = DB.withTransaction { implicit session =>
    Tokens.queryByUUID(uuid).delete
  }

  def deleteExpiredTokens(): Unit = DB.withTransaction { implicit session =>
    Tokens.deleteExpired()
  }
}

package models

import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import securesocial.core._
import play.api.libs.Codecs

import scala.language.implicitConversions
import scala.language.postfixOps

case class User(
  id: Option[Long],
  identityId: IdentityId,
  firstName: String,
  lastName: String,
  email: Option[String],
  avatarUrl: Option[String],
  authMethod: AuthenticationMethod,
  oAuth1Info: Option[OAuth1Info],
  oAuth2Info: Option[OAuth2Info],
  passwordInfo: Option[PasswordInfo]
) extends Identity {

  def fullName: String = s"$firstName $lastName"
  def avatar: Option[String] = avatarUrl.orElse {
    email.map { e => s"http://www.gravatar.com/avatar/${Codecs.md5(e.getBytes)}.png" }
  }

  def updateIdentity(i: Identity): User = {
    this.copy(
      identityId = i.identityId,
      firstName = i.firstName,
      lastName = i.lastName,
      email = i.email,
      authMethod = i.authMethod,
      avatarUrl = i.avatarUrl,
      oAuth1Info = i.oAuth1Info,
      oAuth2Info = i.oAuth2Info,
      passwordInfo = i.passwordInfo
    )
  }
}

object User {

  def apply(i: Identity): User = {
    new User(
      id = None,
      identityId = i.identityId,
      firstName = i.firstName,
      lastName = i.lastName,
      email = i.email,
      authMethod = i.authMethod,
      avatarUrl = i.avatarUrl,
      oAuth1Info = i.oAuth1Info,
      oAuth2Info = i.oAuth2Info,
      passwordInfo = i.passwordInfo
    )
  }

  implicit val jsonWrites = new Writes[User] {
    def writes(o: User): JsValue = {
      Json.obj(
        "email" -> o.email
      )
    }
  }
}

class UserTable(tag: Tag) extends Table[User](tag, "users") {

  implicit val AuthenticationMethodColumnType = MappedColumnType.base[AuthenticationMethod, String](_.method, AuthenticationMethod(_))

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[String]("userId")
  def providerId = column[String]("providerId")
  def firstName = column[String]("firstName")
  def lastName = column[String]("lastName")
  def email = column[Option[String]]("email")
  def avatarUrl = column[Option[String]]("avatarUrl")
  def authMethod = column[AuthenticationMethod]("authMethod")

  def oAuth1InfoToken = column[Option[String]]("oAuth1InfoToken")
  def oAuth1InfoSecret = column[Option[String]]("oAuth1InfoSecret")

  def oAuth2InfoAccessToken = column[Option[String]]("oAuth1InfoAccessToken")
  def oAuth2InfoTokenType = column[Option[String]]("oAuth1InfoTokenType")
  def oAuth2InfoExpiresIn = column[Option[Int]]("oAuth1InfoExpiresIn")
  def oAuth2InfoRefreshToken = column[Option[String]]("oAuth1InfoRefreshToken")

  def passwordInfoHasher = column[Option[String]]("passwordInfoHasher")
  def passwordInfoPassword = column[Option[String]]("passwordInfoPassword")
  def passwordInfoSalt = column[Option[String]]("passwordInfoSalt")


  def userIdProviderIdIndex = index("userprovider_index", (userId, providerId), unique = true)
  def emailIndex = index("email_index", email, unique = true)

  implicit def tuple2IdentityId(tuple: (String, String)) = tuple match {
    case (userId, providerId) => IdentityId(userId, providerId)
  }

  implicit def tuple2OAuth1Info(tuple: (Option[String], Option[String])) = tuple match {
    case (Some(token), Some(secret)) => Some(OAuth1Info(token, secret))
    case _ => None
  }

  implicit def tuple2OAuth2Info(tuple: (Option[String], Option[String], Option[Int], Option[String])) = tuple match {
    case (Some(token), tokenType, expiresIn, refreshToken) => Some(OAuth2Info(token, tokenType, expiresIn, refreshToken))
    case _ => None
  }

  implicit def tuple2PasswordInfo(tuple: (Option[String], Option[String], Option[String])) = tuple match {
    case (Some(hasher), Some(password), salt) => Some(PasswordInfo(hasher, password, salt))
    case _ => None
  }

  private def columnsToUser(
    id: Option[Long],
    userId: String,
    providerId: String,
    firstName: String,
    lastName: String,
    email: Option[String],
    avatarUrl: Option[String],
    authMethod: AuthenticationMethod,

    oAuth1InfoToken: Option[String],
    oAuth1InfoSecret: Option[String],

    oAuth2InfoAccessToken: Option[String],
    oAuth2InfoTokenType: Option[String],
    oAuth2InfoExpiresIn: Option[Int],
    oAuth2InfoRefreshToken: Option[String],

    passwordInfoHasher: Option[String],
    passwordInfoPassword: Option[String],
    passwordInfoSalt: Option[String]
  ): User = {
    User(
      id,
      (userId, providerId),
      firstName,
      lastName,
      email,
      avatarUrl,
      authMethod,
      (oAuth1InfoToken, oAuth1InfoSecret),
      (oAuth2InfoAccessToken, oAuth2InfoTokenType, oAuth2InfoExpiresIn, oAuth2InfoRefreshToken),
      (passwordInfoHasher, passwordInfoPassword, passwordInfoSalt)
    )
  }

  private def userToColumns(u: User) = {
    Some((
      u.id,

      u.identityId.userId,
      u.identityId.providerId,

      u.firstName,
      u.lastName,
      u.email,
      u.avatarUrl,
      u.authMethod,

      u.oAuth1Info.map(_.token),
      u.oAuth1Info.map(_.secret),

      u.oAuth2Info.map(_.accessToken),
      u.oAuth2Info.flatMap(_.tokenType),
      u.oAuth2Info.flatMap(_.expiresIn),
      u.oAuth2Info.flatMap(_.refreshToken),

      u.passwordInfo.map(_.hasher),
      u.passwordInfo.map(_.password),
      u.passwordInfo.flatMap(_.salt)
    ))
  }

  def * = (
    id.?,

    userId,
    providerId,

    firstName,
    lastName,
    email,
    avatarUrl,
    authMethod,

    oAuth1InfoToken,
    oAuth1InfoSecret,

    oAuth2InfoAccessToken,
    oAuth2InfoTokenType,
    oAuth2InfoExpiresIn,
    oAuth2InfoRefreshToken,

    passwordInfoHasher,
    passwordInfoPassword,
    passwordInfoSalt
  ) <> (columnsToUser _ tupled, userToColumns)
}

object Users extends TableQuery(new UserTable(_)) {

  // Queries
  val queryByUserIdAndProviderId = Compiled((userId: Column[String], providerId: Column[String]) =>
    Users.filter(u => u.userId === userId && u.providerId === providerId)
  )

  val queryByEmailAndProviderId = Compiled((email: Column[String], providerId: Column[String]) => {
    Users.filter(u => u.email === email && u.providerId === providerId)
  })

  val queryByEmail = Compiled((email: Column[String]) => {
    Users.filter(u => u.email === email)
  })

  // Finders
  def findByIdentityId(identityId: IdentityId)(implicit session: Session): Option[User] = {
    Users.queryByUserIdAndProviderId(identityId.userId, identityId.providerId).firstOption
  }

  def findByEmailAndProviderId(email: String, providerId: String)(implicit session: Session): Option[User] = {
    queryByEmailAndProviderId(email, providerId).firstOption
  }

  def findByEmail(email: String)(implicit session: Session): Option[User] = {
    queryByEmail(email).firstOption
  }

  def save(i: Identity)(implicit session: Session): Identity = {
    // If the identity has an email use that, otherwise fallback to userid and providerid
    val query = i.email.map(queryByEmail(_)).getOrElse(queryByUserIdAndProviderId(i.identityId.userId, i.identityId.providerId))

    query.firstOption match {
      case Some(user) => {
        query.update(user.updateIdentity(i))
        i
      }
      case None => {
        val user = User(i)
        val id = (Users returning Users.map(_.id)) += User(i)
        user.copy(id = Some(id))
      }
    }
  }
}

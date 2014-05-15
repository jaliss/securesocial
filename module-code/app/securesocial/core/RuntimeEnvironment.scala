package securesocial.core

import securesocial.controllers.{MailTemplates, ViewTemplates}
import securesocial.core.providers.utils.{PasswordValidator, PasswordHasher, Mailer}
import securesocial.core.authenticator._
import securesocial.core.services._
import securesocial.core.providers._
import scala.Some

/**
 * A runtime environment where the services needed are available
 */
trait RuntimeEnvironment[U] {
  val routes: RoutesService

  val viewTemplates: ViewTemplates
  val mailTemplates: MailTemplates

  val mailer: Mailer

  val currentHasher: PasswordHasher
  val passwordHashers: Map[String, PasswordHasher]
  val passwordValidator: PasswordValidator

  val httpService: HttpService
  val cacheService: CacheService
  val avatarService: Option[AvatarService]

  val providers: Map[String, IdentityProvider]

  val idGenerator: IdGenerator
  val authenticatorService: AuthenticatorService[U]

  val eventListeners: List[EventListener[U]]

  val userService: UserService[U]
}

object RuntimeEnvironment {

  /**
   * A default runtime environment.  All built in services are included.
   * You can start your app with with by only adding a userService to handle users.
   */
  abstract class Default[U] extends RuntimeEnvironment[U] {
    override lazy val routes: RoutesService = new RoutesService.Default()

    override lazy val viewTemplates: ViewTemplates = new ViewTemplates.Default(this)
    override lazy val mailTemplates: MailTemplates = new MailTemplates.Default(this)
    override lazy val mailer: Mailer = new Mailer.Default(mailTemplates)

    override lazy val currentHasher: PasswordHasher = new PasswordHasher.Default()
    override lazy val passwordHashers: Map[String, PasswordHasher] = Map(currentHasher.id -> currentHasher)
    override lazy val passwordValidator: PasswordValidator = new PasswordValidator.Default()

    override lazy val httpService: HttpService = new HttpService.Default()
    override lazy val cacheService: CacheService = new CacheService.Default()
    override lazy val avatarService: Option[AvatarService] = Some(new AvatarService.Default(httpService))
    override lazy val idGenerator: IdGenerator = new IdGenerator.Default()

    override lazy val authenticatorService = new AuthenticatorService(
      new CookieAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator),
      new HttpHeaderAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator)
    )

    override lazy val eventListeners: List[EventListener[U]] = List()

    protected def include(p: IdentityProvider) = p.id -> p

    override lazy val providers = Map(
      // oauth 2 client providers
      include(new FacebookProvider(routes, httpService, cacheService)),
      include(new FoursquareProvider(routes, httpService, cacheService)),
      include(new GitHubProvider(routes, httpService, cacheService)),
      include(new GoogleProvider(routes, httpService, cacheService)),
      include(new InstagramProvider(routes, httpService, cacheService)),
      // include(new LinkedInOAuth2Provider(routes, httpService, cacheService)),
      include(new VkProvider(routes, httpService, cacheService)),
      // oauth 1 client providers
      include(new LinkedInProvider(routes, httpService, cacheService)),
      include(new TwitterProvider(routes, httpService, cacheService)),
      // username password
      include(new UsernamePasswordProvider[U](this))
    )
  }
}

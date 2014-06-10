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
    protected def oauth1ClientFor(provider: String) = new OAuth1Client.Default(ServiceInfoHelper.forProvider(TwitterProvider.Twitter), httpService)
    protected def oauth2ClientFor(provider: String) = new OAuth2Client.Default(httpService, OAuth2Settings.forProvider(provider))

    override lazy val providers = Map(
      // oauth 2 client providers
      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
      include(new FoursquareProvider(routes, cacheService,oauth2ClientFor(FoursquareProvider.Foursquare))),
      include(new GitHubProvider(routes, cacheService,oauth2ClientFor(GitHubProvider.GitHub))),
      include(new GoogleProvider(routes, cacheService,oauth2ClientFor(GoogleProvider.Google))),
      include(new InstagramProvider(routes, cacheService,oauth2ClientFor(InstagramProvider.Instagram))),
      //include(new LinkedInOAuth2Provider(routes, cacheService,oauth2ClientFor(LinkedInOAuth2Provider.LinkedIn))),
      include(new VkProvider(routes, cacheService,oauth2ClientFor(VkProvider.Vk))),
      // oauth 1 client providers
      include(new LinkedInProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))),
      include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))),
      // username password
      include(new UsernamePasswordProvider[U](this))
    )
  }
}

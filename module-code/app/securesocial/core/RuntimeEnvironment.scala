package securesocial.core

import akka.actor.ActorSystem
import play.api.{ Configuration, Environment }
import play.api.cache.AsyncCacheApi
import play.api.i18n.MessagesApi
import securesocial.controllers.{ MailTemplates, ViewTemplates }
import securesocial.core.authenticator._
import securesocial.core.providers._
import securesocial.core.providers.utils.{ Mailer, PasswordHasher, PasswordValidator }
import securesocial.core.services._

import scala.concurrent.ExecutionContext
import scala.collection.immutable.ListMap
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient
import play.api.mvc.PlayBodyParsers
/**
 * A runtime environment where the services needed are available
 */
trait RuntimeEnvironment {

  type U

  def routes: RoutesService

  def viewTemplates: ViewTemplates
  def mailTemplates: MailTemplates

  def mailer: Mailer

  def currentHasher: PasswordHasher
  def passwordHashers: Map[String, PasswordHasher]
  def passwordValidator: PasswordValidator

  def httpService: HttpService
  def cacheService: CacheService
  def avatarService: Option[AvatarService]

  def providers: Map[String, IdentityProvider]

  def idGenerator: IdGenerator
  def authenticatorService: AuthenticatorService[U]

  def eventListeners: Seq[EventListener]

  def userService: UserService[U]

  implicit def executionContext: ExecutionContext

  def configuration: Configuration
  lazy val usernamePasswordConfig: UsernamePasswordConfig =
    UsernamePasswordConfig.fromConfiguration(configuration)
  lazy val httpHeaderConfig: HttpHeaderConfig =
    HttpHeaderConfig.fromConfiguration(configuration)
  lazy val cookieConfig: CookieConfig =
    CookieConfig.fromConfiguration(configuration)
  lazy val enableRefererAsOriginalUrl: EnableRefererAsOriginalUrl =
    EnableRefererAsOriginalUrl(configuration)
  lazy val registrationEnabled =
    RegistrationEnabled(configuration)

  def messagesApi: MessagesApi

  def parsers: PlayBodyParsers
}

object RuntimeEnvironment {

  /**
   * A default runtime environment.  All built in services are included.
   * You can start your app with with by only adding a userService to handle users.
   */
  abstract class Default extends RuntimeEnvironment {
    def wsClient: WSClient
    def cacheApi: AsyncCacheApi
    def environment: Environment
    def mailerClient: MailerClient
    def parsers: PlayBodyParsers
    def actorSystem: ActorSystem

    override lazy val routes: RoutesService = new RoutesService.Default(environment, configuration)

    override lazy val viewTemplates: ViewTemplates = new ViewTemplates.Default(this)(configuration)
    override lazy val mailTemplates: MailTemplates = new MailTemplates.Default(this)
    override lazy val mailer: Mailer = new Mailer.Default(mailTemplates, mailerClient, configuration, actorSystem)

    override lazy val currentHasher: PasswordHasher = new PasswordHasher.Default(configuration)
    override lazy val passwordHashers: Map[String, PasswordHasher] = Map(currentHasher.id -> currentHasher)
    override lazy val passwordValidator: PasswordValidator = new PasswordValidator.Default(usernamePasswordConfig.minimumPasswordLength)

    override lazy val httpService: HttpService = new HttpService.Default(wsClient)
    override lazy val cacheService: CacheService = new CacheService.Default(cacheApi)
    override lazy val avatarService: Option[AvatarService] = Some(new AvatarService.Default(httpService))
    override lazy val idGenerator: IdGenerator = new IdGenerator.Default(configuration)

    override lazy val authenticatorService: AuthenticatorService[U] = new AuthenticatorService(
      new CookieAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator, cookieConfig),
      new HttpHeaderAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator, httpHeaderConfig))

    override lazy val eventListeners: Seq[EventListener] = Seq()

    protected def include(p: IdentityProvider): (String, IdentityProvider) = p.id -> p
    protected def oauth1ClientFor(provider: String): OAuth1Client =
      new OAuth1Client.Default(ServiceInfoHelper.forProvider(configuration, provider), httpService)
    protected def oauth2ClientFor(provider: String): OAuth2Client =
      new OAuth2Client.Default(httpService, OAuth2Settings.forProvider(configuration, provider))

    protected lazy val builtInProviders = ListMap(
      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
      include(new FoursquareProvider(routes, cacheService, oauth2ClientFor(FoursquareProvider.Foursquare))),
      include(new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub))),
      include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
      include(new InstagramProvider(routes, cacheService, oauth2ClientFor(InstagramProvider.Instagram))),
      include(new ConcurProvider(routes, cacheService, oauth2ClientFor(ConcurProvider.Concur))),
      include(new SoundcloudProvider(routes, cacheService, oauth2ClientFor(SoundcloudProvider.Soundcloud))),
      include(new LinkedInOAuth2Provider(routes, cacheService, oauth2ClientFor(LinkedInOAuth2Provider.LinkedIn))),
      include(new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk))),
      include(new DropboxProvider(routes, cacheService, oauth2ClientFor(DropboxProvider.Dropbox))),
      include(new WeiboProvider(routes, cacheService, oauth2ClientFor(WeiboProvider.Weibo))),
      include(new ConcurProvider(routes, cacheService, oauth2ClientFor(ConcurProvider.Concur))),
      include(new SpotifyProvider(routes, cacheService, oauth2ClientFor(SpotifyProvider.Spotify))),
      include(new SlackProvider(routes, cacheService, oauth2ClientFor(SlackProvider.Slack))),
      // oauth 1 client providers
      //include(new LinkedInProvider(routes, cacheService, oauth1ClientFor(LinkedInProvider.LinkedIn))),
      include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))),
      include(new XingProvider(routes, cacheService, oauth1ClientFor(XingProvider.Xing))),
      // username password
      include(new UsernamePasswordProvider[U](userService, avatarService, viewTemplates, passwordHashers, messagesApi)))

    override lazy val providers: ListMap[String, IdentityProvider] = builtInProviders
  }
}

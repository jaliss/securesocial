package securesocial.core

import securesocial.controllers.{ MailTemplates, ViewTemplates }
import securesocial.core.authenticator._
import securesocial.core.providers._
import securesocial.core.providers.utils.{ Mailer, PasswordHasher, PasswordValidator }
import securesocial.core.services._

import scala.concurrent.ExecutionContext
import scala.collection.immutable.ListMap

import play.api.libs.concurrent.{ Execution => PlayExecution }
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

  /**
   * Factory method for IdentityProvider
   * @param provider provider name e.g. "github"
   * @param customOAuth2Settings Valid only for OAuth2Provider. If None, the default settings are used.
   * @return
   */
  def createProvider(provider: String, customOAuth2Settings: Option[OAuth2Settings] = None): IdentityProvider = {
    provider match {
      case FacebookProvider.Facebook =>
        new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook, customOAuth2Settings))
      case FoursquareProvider.Foursquare =>
        new FoursquareProvider(routes, cacheService, oauth2ClientFor(FoursquareProvider.Foursquare, customOAuth2Settings))
      case GitHubProvider.GitHub =>
        new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub, customOAuth2Settings))
      case GoogleProvider.Google =>
        new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google, customOAuth2Settings))
      case InstagramProvider.Instagram =>
        new InstagramProvider(routes, cacheService, oauth2ClientFor(InstagramProvider.Instagram, customOAuth2Settings))
      case ConcurProvider.Concur =>
        new ConcurProvider(routes, cacheService, oauth2ClientFor(ConcurProvider.Concur, customOAuth2Settings))
      case SoundcloudProvider.Soundcloud =>
        new SoundcloudProvider(routes, cacheService, oauth2ClientFor(SoundcloudProvider.Soundcloud, customOAuth2Settings))
      case VkProvider.Vk =>
        new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk, customOAuth2Settings))
      case DropboxProvider.Dropbox =>
        new DropboxProvider(routes, cacheService, oauth2ClientFor(DropboxProvider.Dropbox, customOAuth2Settings))
      case WeiboProvider.Weibo =>
        new WeiboProvider(routes, cacheService, oauth2ClientFor(WeiboProvider.Weibo, customOAuth2Settings))
      case SlackProvider.Slack =>
        new SlackProvider(routes, cacheService, oauth2ClientFor(SlackProvider.Slack, customOAuth2Settings))
      case LinkedInProvider.LinkedIn =>
        new LinkedInProvider(routes, cacheService, oauth1ClientFor(LinkedInProvider.LinkedIn))
      case TwitterProvider.Twitter =>
        new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))
      case XingProvider.Xing =>
        new XingProvider(routes, cacheService, oauth1ClientFor(XingProvider.Xing))
      case _ => throw new RuntimeException(s"Invalid provider '$provider'")
    }
  }

  protected def oauth1ClientFor(provider: String) = new OAuth1Client.Default(ServiceInfoHelper.forProvider(provider), httpService)
  protected def oauth2ClientFor(provider: String, customSettings: Option[OAuth2Settings] = None): OAuth2Client = {
    val settings = customSettings.getOrElse(OAuth2Settings.forProvider(provider))
    new OAuth2Client.Default(httpService, settings)
  }
}

object RuntimeEnvironment {

  /**
   * A default runtime environment.  All built in services are included.
   * You can start your app with with by only adding a userService to handle users.
   */
  abstract class Default extends RuntimeEnvironment {
    override lazy val routes: RoutesService = new RoutesService.Default()

    override lazy val viewTemplates: ViewTemplates = new ViewTemplates.Default(this)
    override lazy val mailTemplates: MailTemplates = new MailTemplates.Default(this)
    override lazy val mailer: Mailer = new Mailer.Default(mailTemplates)

    override lazy val currentHasher: PasswordHasher = new PasswordHasher.Default()
    override lazy val passwordHashers: Map[String, PasswordHasher] = Map(currentHasher.id -> currentHasher)
    override lazy val passwordValidator: PasswordValidator = new PasswordValidator.Default()

    override lazy val httpService: HttpService = new HttpService.Default
    override lazy val cacheService: CacheService = new CacheService.Default
    override lazy val avatarService: Option[AvatarService] = Some(new AvatarService.Default(httpService))
    override lazy val idGenerator: IdGenerator = new IdGenerator.Default()

    override lazy val authenticatorService = new AuthenticatorService(
      new CookieAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator),
      new HttpHeaderAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator)
    )

    override lazy val eventListeners: Seq[EventListener] = Seq()
    override implicit def executionContext: ExecutionContext =
      PlayExecution.defaultContext

    protected def include(p: IdentityProvider) = p.id -> p
    override lazy val providers = ListMap(
      // oauth 2 client providers
      include(createProvider(FacebookProvider.Facebook)),
      include(createProvider(FoursquareProvider.Foursquare)),
      include(createProvider(GitHubProvider.GitHub)),
      include(createProvider(GoogleProvider.Google)),
      include(createProvider(InstagramProvider.Instagram)),
      include(createProvider(ConcurProvider.Concur)),
      include(createProvider(SoundcloudProvider.Soundcloud)),
      //include(createProvider(LinkedInOAuth2Provider.LinkedIn)),
      include(createProvider(VkProvider.Vk)),
      include(createProvider(DropboxProvider.Dropbox)),
      include(createProvider(WeiboProvider.Weibo)),
      include(createProvider(ConcurProvider.Concur)),
      include(createProvider(SlackProvider.Slack)),
      // oauth 1 client providers
      include(createProvider(LinkedInProvider.LinkedIn)),
      include(createProvider(TwitterProvider.Twitter)),
      include(createProvider(XingProvider.Xing)),
      // username password
      include(new UsernamePasswordProvider[U](userService, avatarService, viewTemplates, passwordHashers))
    )
  }
}

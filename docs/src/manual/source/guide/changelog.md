---
file: changelog
---
# Change Log

*This is the change log for the SecureSocial 2 version. Check the [1.x](https://github.com/jaliss/securesocial/tree/1.x) branch if you're interested in SecureSocial 1.*

**2.0.5 - 2012-11-14**

- Simplified installation by providing an ivy style repository.
- Updated installation steps in the user guide.
- Added a change log page to the docs.
- Changed the assets directory for SecureSocial to avoid conflicts with apps using it.
- Added Bootstrap to demo apps

**2.0.4 - 2012-11-11**

- Improved signup flow (validates email before allowing registration)
- Added reset password functionality
- Added email notifications
- Added a way to customise views  & mails (TemplatesPlugin)
- Added a setting to enable HTTPS in the URLSs for OAUTH callbacks and routes
- Replaced displayName in SocialUser for firstName, lastName and fullName fields
- Added RoutesHelper to build urls using the routes file of the app using the module
- Added a user guide

**2.0.3 - 2012-11-02**

- Fix to prevent not serializable exception in OAuth1Provider (thanks to @chazmcgarvey)

**2.0.2 - 2012-09-24**

- Added GitHub provider
- Changed FacebookProvider to use the new Facebook API (user picture)
- Moved to Play 2.0.3
- Fixed #71: Context not available in UserAwareAction

**2.0.1 - 2012-06-22**

- Added LinkedIn provider

**2.0.0   - 2012-06-05**

- Initial release of SecureSocial for Play 2 published
- Facebook, Google and Twitter providers



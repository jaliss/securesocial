package securesocial.core

/**
 * A trait to define Authorization objects that let you hook
 * an authorization implementation in SecuredActions
 *
 */
trait Authorization {
  /**
   * Checks whether the user is authorized to execute an action or not.
   *
   * @param user
   * @return
   */
  def isAuthorized(user: Identity): Boolean
}

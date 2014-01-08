package securesocial.core.providers.utils

import play.api.data.Mapping
import play.api.data.validation._

class ExtendedMapping[T](val m: Mapping[T]) {

  /**
   * Constructs a new Mapping based on this one, by adding a new ad-hoc constraint.
   *
   * For example:
   * {{{
   *   import play.api.data._
   *   import validation.Constraints._
   *   import securesocial.core.providers.utils._
   *
   *   Form("field" -> text.verifying(ValidationError("message", parameter), {!_.isOK}))
   * }}}
   *
   * @param error The validation error used if the constraint fails
   * @param constraint a function describing the constraint that returns `false` on failure
   * @return the new mapping
   */
  def verifyingValidationError(error: => ValidationError, constraint: (T => Boolean)): Mapping[T] = {
    m.verifying(Constraint { t: T =>
      if (constraint(t)) Valid else Invalid(Seq(error))
    })
  }
}

object ExtendedMappingConverter {
  implicit def mappingToExtendedMapping[T](m: Mapping[T]) = new ExtendedMapping[T](m)
}

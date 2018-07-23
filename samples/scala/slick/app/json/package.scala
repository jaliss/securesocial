import play.api.libs.json._
import securesocial.core.Identity

package object json {

  implicit val jsonIdentity = new Writes[Identity] {
    def writes(o: Identity): JsValue = {
      Json.obj(
        "email" -> o.email,
        "firstName" -> o.firstName,
        "lastName" -> o.lastName,
        "avatarUrl" -> o.avatarUrl
      )
    }
  }

}

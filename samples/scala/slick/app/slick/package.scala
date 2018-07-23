import org.joda.time.DateTime
import java.sql.Date
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

package object slick {

  implicit val dateTimeColumnType = MappedColumnType.base[DateTime, Date](
    dateTime => new Date(dateTime.getMillis),
    date => new DateTime(date)
  )

}

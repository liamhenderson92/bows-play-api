package mongoDateTimeFormats

import java.time._
import play.api.libs.json._

trait MongoDateTimeFormat {

  implicit val localDateTimeRead: Reads[LocalDateTime] =
    (__ \ "$date").read[Long].map {
      millis =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
    }

  implicit val localDateTimeWrite: Writes[LocalDateTime] =     (dateTime: LocalDateTime) =>
      Json.obj("$date" -> dateTime.atZone(ZoneOffset.ofHours(1)).toInstant.toEpochMilli
  )
}

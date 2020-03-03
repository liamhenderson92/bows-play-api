package models

import java.time.LocalDateTime
import play.api.libs.json._
import mongoDateTimeFormats.MongoDateTimeFormat

case class MemberSession(_id: String, lastUpdated: LocalDateTime)

object MemberSession extends MongoDateTimeFormat {
  implicit lazy val format: OFormat[MemberSession] = Json.format
}

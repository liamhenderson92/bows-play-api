package models

import java.time.LocalDateTime

import mongoDateTimeFormats.MongoDateTimeFormat
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json

class MemberSessionSpec extends FreeSpec with MustMatchers with MongoDateTimeFormat {

  "MemberSession model" - {

    val id = "r7jTG7dqBy5wGO4L"
    val timeNow = LocalDateTime.now

    "must serialise into JSON correctly" in {
      val memberSession = MemberSession(
        _id = id,
        lastUpdated = timeNow
      )

      val expectedJson = Json.obj(
        "_id" -> id,
        "lastUpdated" -> timeNow
      )
      Json.toJson(memberSession) mustEqual expectedJson
    }

    "must deserialise from JSON correctly" in {
      val json = Json.obj(
        "_id" -> id,
        "lastUpdated" -> timeNow
      )

      val expectedUser = MemberSession(
        _id = id,
        lastUpdated = timeNow.minusHours(1)
      )
      json.as[MemberSession] mustEqual expectedUser
    }
  }
}

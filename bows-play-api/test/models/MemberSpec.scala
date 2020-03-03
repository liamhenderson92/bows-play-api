package models

import org.scalatest._
import play.api.libs.json.Json

class MemberSpec extends WordSpec with OptionValues with MustMatchers {

  val card: Card = Card("TEST12345")

  "Member model" must {
    "Deserialise correctly" in {
      val json = Json.obj(
        "_id" -> "TEST12345",
        "name" -> "liam",
        "email" -> "liam@gmail.com",
        "mobileNumber" -> "1234567890",
        "funds" -> 100,
        "pin" -> 1234
      )

      val expectedMember = Member(
        card = card,
        name = "liam",
        email = "liam@gmail.com",
        mobileNumber = "1234567890",
        funds = 100,
        pin = 1234
      )
      json.as[Member] mustEqual expectedMember
    }

    "Serialise correctly" in {
      val member = Member(
        card = card,
        name = "liam",
        email = "liam@gmail.com",
        mobileNumber = "1234567890",
        funds = 100,
        pin = 1234
      )

      val expectedJson = Json.obj(
        "_id" -> "TEST12345",
        "name" -> "liam",
        "email" -> "liam@gmail.com",
        "mobileNumber" -> "1234567890",
        "funds" -> 100,
        "pin" -> 1234
      )
      Json.toJson(member) mustBe expectedJson
    }
  }

}

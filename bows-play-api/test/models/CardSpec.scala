package models

import models.Card._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class CardSpec extends WordSpec with OptionValues with MustMatchers {

  val validCardID = "r7jTG7dqBy5wGO4L"

  "Card model" must {

    "Deserialse correctly" in {
      val cardId = Card(_id = validCardID)
      val expectedJson = Json.obj("_id" -> validCardID)

      Json.toJson(cardId) mustEqual expectedJson
    }

    "Serialise correctly" in {
      val expectedCardId = Card(_id = validCardID)
      val json = Json.obj("_id" -> validCardID)

      json.as[Card] mustEqual expectedCardId
    }

    "return 'Card ID provided is invalid' if the card ID does not match the regex" in {
      val invalidCardID = "WRONGID12345"
      val result = "Card ID provided is invalid"

      pathBindable.bind("", invalidCardID) mustBe Left(result)
    }

    "return a String" in {
      pathBindable.unbind("", Card("TEST12345")) mustEqual "TEST12345"
    }
  }

}

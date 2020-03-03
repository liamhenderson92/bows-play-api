package models

import play.api.libs.json._
import play.api.mvc.PathBindable

case class Card(_id: String)

object Card {

  implicit val read: Reads[Card] = (__ \ "_id").read[String].map(Card(_))
  implicit val write: OWrites[Card] = (__ \ "_id").write[String].contramap(_._id)

  implicit val pathBindable: PathBindable[Card] = {
    new PathBindable[Card] {
      override def bind(key: String, value: String): Either[String, Card] =
        if (value.matches("^[0-9a-zA-Z]{16}$")) Right(Card(value))
        else Left("Card ID provided is invalid")

      override def unbind(key: String, value: Card): String = value._id
    }
  }

}

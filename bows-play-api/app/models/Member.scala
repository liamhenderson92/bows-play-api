package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Member (
                  card: Card,
                  name: String,
                  email: String,
                  mobileNumber: String,
                  funds: Double,
                  pin: Int
                  )

object Member {

  implicit val reads: Reads[Member] =
    (__.read[Card] and
      (__ \ "name").read[String] and
      (__ \ "email").read[String] and
      (__ \ "mobileNumber").read[String] and
      (__ \ "funds").read[Double] and
      (__ \ "pin").read[Int]) (Member.apply _)

  implicit val write: OWrites[Member] =
    (__.write[Card] and
      (__ \ "name").write[String] and
      (__ \ "email").write[String] and
      (__ \ "mobileNumber").write[String] and
      (__ \ "funds").write[Double] and
      (__ \ "pin").write[Int]) (unlift(Member.unapply _))

}

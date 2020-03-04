package controllers

import java.time.LocalDateTime

import models.{Card, Member, MemberSession}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import repositories.{MemberRepository, SessionRepository}

import scala.concurrent.Future

class MemberControllerSpec extends WordSpec with MustMatchers with MockitoSugar with ScalaFutures {

  val mockMemberRespository: MemberRepository = mock[MemberRepository]
  val mockSessionRespository: SessionRepository = mock[SessionRepository]

  private lazy val builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().overrides(
      bind[MemberRepository].toInstance(mockMemberRespository),
      bind[SessionRepository].toInstance(mockSessionRespository)
    )

  private val card = Card("testId0123456789")

  "activateCard" must {
    "return an OK response and delete current session if a session already exists" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockSessionRespository.findSession(any()))
        .thenReturn(Future.successful(Some(MemberSession("testId0123456789", LocalDateTime.now))))

      when(mockSessionRespository.deleteSession(any()))
        .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.activateCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Goodbye - testName."

      app.stop
    }

    "return an OK response and create a new session if a session does not exist" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockSessionRespository.findSession(any()))
        .thenReturn(Future.successful(None))

      when(mockSessionRespository.createSession(any()))
        .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.activateCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Welcome - testName."

      app.stop
    }

    "return a BAD_REQUEST response and the correct message if the member searched does not exist" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.activateCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Your card is not registered - please register"

      app.stop
    }

    "return a BAD_REQUEST response if the data provided is invalid" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.failed(JsResultException(Seq.empty)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.activateCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data format - unable to parse Json data to model"

      app.stop
    }

    "return a BAD_REQUEST response for any other failure" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.activateCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Issue occurred - exception: java.lang.Exception"

      app.stop
    }
  }

  "findMember" must {
    "return an OK response and show the member details" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))
      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMember(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) must contain
      """{"_id":card,"name":testName,"email":"testEmailAddress","mobileNumber":"testMobileNumber","funds":100,"pin":1234}""".stripMargin

      app.stop
    }

    "return a NOT_FOUND response and message when the member searched for could not be found" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.successful(None))
      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMember(Card("INCORRECTID12345")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card ID"

      app.stop
    }

    "return a BAD_REQUEST response if the data provided is invalid" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.failed(JsResultException(Seq.empty)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMember(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data format - unable to parse Json data to model"

      app.stop
    }

    "return a BAD_REQUEST response for any other failure" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMember(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Issue occurred - exception: java.lang.Exception"

      app.stop
    }
  }

  "registerMember" must {
    "return an OK response with message when given the correct data" in {
      when(mockMemberRespository.registerMember(any()))
        .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

      val memberJson: JsValue = Json.toJson(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))

      val app: Application = builder.build()

      val request: FakeRequest[JsValue] =
        FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Member registered successfully!"

      app.stop
    }

    "Return a BAD_REQUEST response with error message when the data is invalid" in {
      val memberJson: JsValue = Json.toJson("Invalid Json")

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data format - unable to parse Json data to model"

      app.stop
    }

    "Return a BAD_REQUEST response with error message when given duplicate data" in {
      when(mockMemberRespository.registerMember(any()))
        .thenReturn(Future.failed(new DatabaseException {
          override def originalDocument: Option[BSONDocument] = None

          override def code: Option[Int] = None

          override def message: String = "Duplicate key - unable to parse Json to the Member model"
        }))

      val memberJson: JsValue = Json.toJson(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Duplicate key - unable to parse Json to the Member model"

      app.stop
    }

    "Return a BAD_REQUEST response with error message for any other failure" in {
      when(mockMemberRespository.registerMember(any()))
        .thenReturn(Future.failed(new Exception))

      val memberJson: JsValue = Json.toJson(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Issue occurred - exception: java.lang.Exception"

      app.stop
    }
  }

  "addFunds" must {

    "return an OK response with message if data is valid" in {
      when(mockMemberRespository.addFunds(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockMemberRespository.findMember(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.addFunds(Card("testId0123456789"), 234).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Your funds have been added"

      app.stop
    }

    "return a BAD_REQUEST response with error message if given a negative amount" in {

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.addFunds(Card("testId0123456789"), -300).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Please enter an amount over 0.00"

      app.stop
    }

    "return a NOT_FOUND response with error message when the member could not be found" in {

      when(mockMemberRespository.findMember(Card("INCORRECTID12345")))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.addFunds(Card("INCORRECTID12345"), 100).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card ID"

      app.stop
    }
  }

  "checkFunds" must {
    "return a NOT_FOUND response with message when member could not be found" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.checkFunds
      (Card("testId1234567890")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card ID"

      app.stop
    }

    "return an OK response with correct funds amount when given the correct card ID" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController
        .checkFunds(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "100"

      app.stop
    }

    "return a BAD_REQUEST response if the data provided is invalid" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.failed(JsResultException(Seq.empty)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.checkFunds(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data format - unable to parse Json data to model"

      app.stop
    }

    "return a BAD_REQUEST response for any other failure" in {
      when(mockMemberRespository.findMember(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.checkFunds(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Issue occurred - exception: java.lang.Exception"

      app.stop
    }
  }

  "transaction" must {
    "return an OK response with message if the data provided is valid" in {
      when(mockMemberRespository.transaction(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockMemberRespository.findMember(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))


      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.transaction(Card("testId0123456789"), 100).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Your transaction was successful"

      app.stop
    }

    "return a BAD_REQUEST response with error message if the transaction cost is higher than the total funds" in {
      when(mockMemberRespository.findMember(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockMemberRespository.transaction(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.transaction(Card("testId0123456789"), 300).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Error - you do not have enough funds for this"

      app.stop
    }

    "return a NOT_FOUND response with error message when the member could not be found" in {
      when(mockMemberRespository.findMember(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockMemberRespository.findMember(Card("INCORRECTID12345")))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.transaction(Card("INCORRECTID12345"), 100).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card ID"

      app.stop
    }
  }

  "removeMember" must {
    "return an OK response with message if the data provided is valid" in {
      when(mockMemberRespository.removeMember(any()))
        .thenReturn(Future.successful(Some(Json.obj(
          "_id" -> card,
          "name" -> "testName",
          "email" -> "testEmailAddress",
          "mobileNumber" -> "testNumber",
          "funds" -> 100,
          "pin" -> 1234
        ))))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, routes.MemberController.removeMember(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Member removed successfully"

      app.stop
    }

    "return a NOT_FOUND response with error message when the member could not be found" in {
      when(mockMemberRespository.removeMember(any()))
        .thenReturn(Future.successful(None
        ))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, routes.MemberController.removeMember(Card("testId0123456789")).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card ID"

      app.stop
    }

    "return a BAD_REQUEST response for any other failure" in {
      when(mockMemberRespository.removeMember(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, routes.MemberController.removeMember(Card("testId0123456789")).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Issue occurred - exception: java.lang.Exception"

      app.stop
    }
  }

  "updateName" must {
    "return an OK response with message when given correct data" in {
      when(mockMemberRespository.updateName(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockMemberRespository.findMember(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))


      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.updateName(Card("testId0123456789"), "Liam").url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "The name stored for the ID testId0123456789 has been updated to Liam"

      app.stop
    }

    "return a NOT_FOUND response with error message when the member could not be found" in {
      when(mockMemberRespository.updateName(any, any))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateName(Card("INCORRECTID12345"), "Liam").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card ID"

      app.stop
    }

    "return a BAD_REQUEST response with error message for any other failure" in {
      when(mockMemberRespository.updateName(any, any))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateName(Card("INCORRECTID12345"), "Liam").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Issue occurred - exception: java.lang.Exception"

      app.stop
    }
  }

  "updateMobileNumber" must {
    "return an OK response with message when data is valid" in {
      when(mockMemberRespository.updateMobileNumber(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockMemberRespository.findMember(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.updateMobileNumber(Card("testId0123456789"), "07123456789").url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "The mobile number stored for the ID testId0123456789 has been updated to 07123456789"

      app.stop
    }

    "return a NOT_FOUND response with error message when the member could not be found" in {
      when(mockMemberRespository.updateMobileNumber(any, any))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateMobileNumber(Card("INCORRECTID12345"), "07123456789").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card ID"

      app.stop
    }

    "return a BAD_REQUEST response with error message for any other failure" in {
      when(mockMemberRespository.updateMobileNumber(any, any))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateMobileNumber(Card("INCORRECTID12345"), "07123456789").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Issue occurred - exception: java.lang.Exception"

      app.stop
    }
  }

  "updateEmail" must {
    "return an OK response with message when the data provided is valid" in {
      when(mockMemberRespository.updateEmail(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      when(mockMemberRespository.findMember(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmailAddress", "testMobileNumber", 100, 1234))))

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.updateEmail(Card("testId0123456789"), "liamnew@gmail.com").url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "The email stored for ID testId0123456789 has been updated to liamnew@gmail.com"

      app.stop
    }
  }
}

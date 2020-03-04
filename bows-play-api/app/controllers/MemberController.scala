package controllers

import java.time.LocalDateTime

import javax.inject.Inject
import models.{Card, Member, MemberSession}
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc._
import reactivemongo.core.errors.DatabaseException
import repositories.{MemberRepository, SessionRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MemberController @Inject()(cc: ControllerComponents,
                                 memberRepository: MemberRepository,
                                 sessionRepository: SessionRepository)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def activateCard(card: Card): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.findMember(card).flatMap {
        case Some(member) =>
          sessionRepository.findSession(card).flatMap {
            case Some(_) =>
              sessionRepository.deleteSession(card).map(_ => Ok(s"Goodbye - ${member.name}."))
            case None =>
              sessionRepository.createSession(MemberSession(card._id, LocalDateTime.now))
                .map(_ => Ok(s"Welcome - ${member.name}."))
          }
        case None => Future.successful(BadRequest("Your card is not registered - please register"))
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data format - unable to parse Json data to model"))
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e"))
      }
  }

  def findMember(card: Card): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      memberRepository.findMember(card).map {
        case None => NotFound("A member could not be found with that card ID")
        case Some(member) => Ok(Json.toJson(member))
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data format - unable to parse Json data to model"))
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e"))
      }
  }

  def registerMember: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      (for {
        member <- Future.fromTry(Try {
          request.body.as[Member]
        })
        _ <- memberRepository.registerMember(member)
      } yield Ok("Member registered successfully!")).recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data format - unable to parse Json data to model"))
        case _: DatabaseException =>
          Future.successful(BadRequest("Duplicate key - unable to parse Json to the Member model"))
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e"))
      }
  }

  def addFunds(card: Card, increase: Double): Action[AnyContent] = Action.async {
    memberRepository.findMember(card).flatMap {
      case Some(_) =>
        increase match {
          case x if x <= 0 => Future.successful(BadRequest("Please enter an amount over 0.00"))
          case _ =>
            memberRepository.findMember(card).flatMap {
              case Some(_) => memberRepository.addFunds(card, increase)
                .map { _ => Ok("Your funds have been added") }
            }
        }
      case None => Future.successful(NotFound("A member could not be found with that card ID"))
    } recoverWith {
      case _: JsResultException => Future.successful(BadRequest("Incorrect data format - unable to parse Json data to model"))
      case e => Future.successful(BadRequest(s"Issue occurred - exception: $e"))
    }
  }

  def checkFunds(card: Card): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      memberRepository.findMember(card).map {
        case Some(member) => Ok(Json.toJson(member.funds))
        case None => NotFound("A member could not be found with that card ID")
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data format - unable to parse Json data to model"))
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e"))
      }
  }

  def transaction(card: Card, decrease: Double): Action[AnyContent] = Action.async {
    memberRepository.findMember(card).flatMap {
      case Some(member) => {
        decrease match {
          case x if x > member.funds => Future.successful(BadRequest("Error - you do not have enough funds for this"))
          case _ =>
            memberRepository.findMember(card).flatMap {
              case Some(_) =>
                memberRepository.transaction(card, decrease).map {
                  case Some(_) => Ok("Your transaction was successful")
                }
            }
        }
      }
      case None => Future.successful(NotFound("A member could not be found with that card ID"))
    }.recoverWith {
      case _: JsResultException => Future.successful(BadRequest("Incorrect data format - unable to parse Json data to model"))
      case e => Future.successful(BadRequest(s"Issue occurred - exception: $e"))
    }
  }

  def removeMember(card: Card): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.removeMember(card).map {
        case Some(_) => Ok("Member removed successfully")
        case _ => NotFound("A member could not be found with that card ID")
      } recoverWith {
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e"))
      }
  }

  def updateName(card: Card, newName: String): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.updateName(card, newName).map {
        case Some(member) =>
          Ok(s"The name stored for the ID ${member.card._id} has been updated to $newName")
        case _ =>
          NotFound("A member could not be found with that card ID")
      } recoverWith {
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e"))
      }
  }

  def updateMobileNumber(card: Card, newNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.updateMobileNumber(card, newNumber).map {
        case Some(member) =>
          Ok(s"The mobile number stored for the ID ${member.card._id} has been updated to $newNumber")
        case _ =>
          NotFound("A member could not be found with that card ID")
      } recoverWith {
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e"))
      }
  }

  def updateEmail(card: Card, newEmail: String): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.updateEmail(card, newEmail).map {
        case Some(member) =>
          Ok(s"The email stored for ID ${member.card._id} has been updated to $newEmail")
        case _ =>
          NotFound("A member could not be found with that card ID")
      } recoverWith {
        case e =>
          Future.successful(BadRequest(s"Issue occurred - exception: $e")
          )
      }
  }
}

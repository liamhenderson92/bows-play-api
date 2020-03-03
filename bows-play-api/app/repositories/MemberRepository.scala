package repositories

import javax.inject.Inject
import models.{Card, Member}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{FindAndModifyCommand, WriteResult}
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.{JSONCollection, _}

import scala.concurrent.{ExecutionContext, Future}

class MemberRepository @Inject()(cc: ControllerComponents,
                                 config: Configuration,
                                 mongo: ReactiveMongoApi)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val memberCollection: Future[JSONCollection] = {
    mongo.database.map(_.collection[JSONCollection]("members"))
  }

  private def findAndUpdate(collection: JSONCollection, selection: JsObject,
                            modifier: JsObject): Future[FindAndModifyCommand.Result[collection.pack.type]] = {
    collection.findAndUpdate(
      selector = selection,
      update = modifier,
      fetchNewObject = true,
      upsert = false,
      sort = None,
      fields = None,
      bypassDocumentValidation = false,
      writeConcern = WriteConcern.Default,
      maxTime = None,
      collation = None,
      arrayFilters = Seq.empty
    )
  }

  def findMember(card: Card): Future[Option[Member]] = {
    memberCollection.flatMap(_.find(
      Json.obj("_id" -> card._id),
      None
    ).one[Member])
  }

  def registerMember(newMember: Member): Future[WriteResult] = {
    memberCollection.flatMap(_.insert.one(newMember))
  }

  def addFunds(card: Card, increase: Double): Future[Option[Member]] = {
    memberCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> card._id)
        val modifier: JsObject = Json.obj("$inc" -> Json.obj("funds" -> increase))
        findAndUpdate(result, selector, modifier).map(_.result[Member])
    }
  }

  def transaction(card: Card, decrease: Double): Future[Option[Member]] = {
    memberCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> card._id)
        val modifier: JsObject = Json.obj("$inc" -> Json.obj("funds" -> -decrease))
        findAndUpdate(result, selector, modifier).map(_.result[Member])
    }
  }

  def removeMember(card: Card): Future[Option[JsObject]] = {
    memberCollection.flatMap(
      _.findAndRemove(Json.obj("_id" -> card._id), None, None, WriteConcern.Default, None, None, Seq.empty).map(
        _.value
      )
    )
  }

  def updateName(card: Card, newName: String): Future[Option[Member]] = {
    memberCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> card._id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj("name" -> newName))
        findAndUpdate(result, selector, modifier).map(_.result[Member])
    }
  }

  def updateMobileNumber(card: Card, newNumber: String): Future[Option[Member]] = {
    memberCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> card._id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj("mobileNumber" -> newNumber))
        findAndUpdate(result, selector, modifier).map(_.result[Member])
    }
  }

  def updateEmail(card: Card, newEmail: String): Future[Option[Member]] = {
    memberCollection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> card._id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj("email" -> newEmail))
        findAndUpdate(result, selector, modifier).map(_.result[Member])
    }
  }

}
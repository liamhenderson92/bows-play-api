package repositories

import javax.inject.Inject
import models.{Card, MemberSession}
import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.{JSONCollection, _}

import scala.concurrent.{ExecutionContext, Future}

class SessionRepository @Inject()(mongo: ReactiveMongoApi, config: Configuration,
                                  memberRepository: MemberRepository)(implicit ec: ExecutionContext) {

  private val sessionCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("sessions"))

  val timeToLive: Int = config.get[Int]("session.sessionTimeToLive")

  private val index: Index = Index(
    key = Seq("lastUpdated" -> IndexType.Ascending),
    name = Some("session-index"),
    options = BSONDocument("expireAfterSeconds" -> timeToLive)
  )

  sessionCollection.map(_.indexesManager.ensure(index))

  def createSession(session: MemberSession): Future[WriteResult] =
    sessionCollection.flatMap(_.insert.one(session))

  def findSession(card: Card): Future[Option[MemberSession]] =
    sessionCollection.flatMap(_.find(Json.obj("_id" -> card._id), None).one[MemberSession])

  def deleteSession(card: Card): Future[WriteResult] =
    sessionCollection.flatMap(_.delete.one(Json.obj("_id" -> card._id)))
}

package com.epam.onadtochyi.ai.task.conversationmessage

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.epam.onadtochyi.ai.task.ConversationMessageRoutes
import com.epam.onadtochyi.ai.task.conversation.ConversationActionPerfomedStatus
import com.epam.onadtochyi.ai.task.conversationmessage.ConversationMessageRepositoryActor.{ConversationMessageActionPerformed, CreateConversationMessageRequest}
import com.epam.onadtochyi.ai.task.dto.Role
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import slick.jdbc.HsqldbProfile.api._
import com.epam.onadtochyi.ai.task.AiJsonFormats._
import com.epam.onadtochyi.ai.task.dto.Message

class ConversationMessageRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  private lazy val testKit = ActorTestKit()

  implicit def typedSystem: akka.actor.typed.ActorSystem[_] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val convMessDB = Database.forURL(
    url = "jdbc:hsqldb:mem:aitestdb;shutdown=false",
    driver = "org.hsqldb.jdbcDriver"
  )

  val conversationMessageDb = new ConversationMessageDatabase(convMessDB)

  private val conversationMessageRepositoryActor = testKit.spawn(ConversationMessageRepositoryActor(conversationMessageDb))
  private lazy val routes = new ConversationMessageRoutes(conversationMessageRepositoryActor).conversationMessageRoutes

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import io.jvm.uuid._
  import org.scalatest.OptionValues._

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  override def beforeAll(): Unit = {
    super.beforeAll()
    conversationMessageDb.createSchema().futureValue
  }

  override def afterAll(): Unit = {
    convMessDB.close()
    super.afterAll()
  }

  def withCleanDatabase[T](testCode: => T): T = {
    try {
      testCode
    } finally {
      convMessDB.run(TableQuery[ConversationMessageTable].delete).futureValue
    }
  }

  "ConversationMessageRoutes" should {

    "be able to get conversation message by id (GET /conversation/messages/<uuid>)" in withCleanDatabase {
      val messageId = UUID.random

      val conversationMessageRow = ConversationMessageRow(messageId,
        UUID.random, Role.USER, "test message")

      conversationMessageDb.insert(conversationMessageRow).futureValue

//      val messageId = Post("/conversation/message", CreateConversationMessageRequest(UUID.random, "test message", Role.USER)) ~> routes ~> check {
//        responseAs[ConversationMessageActionPerformed].conversationMessage.get.id
//      }

      val request = Get(s"/conversation/messages/$messageId")
      request ~> routes ~> check {
        status should be(StatusCodes.OK)
        contentType shouldBe ContentTypes.`application/json`
        val responseMessage = responseAs[ConversationMessageActionPerformed]
        responseMessage.status should be(ConversationActionPerfomedStatus.DONE)
        responseMessage.conversationMessage.value should be(Message(messageId, Role.USER, "test message"))
      }
    }

    "get an error if uuid format is wrong (GET /conversation/messages/<uuid>)" in {
      val getRequest = Get("/conversation/messages/invalid-uuid")
      getRequest ~> routes ~> check {
        status should be(StatusCodes.BadRequest)
        responseAs[String] should be("Invalid UUID format")
      }
    }

    "get error if uuid format is wrong (GET /conversation/messages/<uuid>)" in {
      val getRequest = Get("/conversation/messages/invalid-uuid")
      getRequest ~> routes ~> check {
        status should be(StatusCodes.BadRequest)
        responseAs[String] should be("Invalid UUID format")
      }
    }

    "be able to add conversation message (POST /conversation/message)" in withCleanDatabase {
      val conversationId = UUID.random
      val request = CreateConversationMessageRequest(conversationId, "test message", Role.USER)

      Post("/conversation/message", request) ~> routes ~> check {
        status should be(StatusCodes.Created)
        val createMessagePerformed = responseAs[ConversationMessageActionPerformed]
        createMessagePerformed.status should be(ConversationActionPerfomedStatus.DONE)
        createMessagePerformed.message should startWith("Message")
        createMessagePerformed.message should endWith(" created.")
        createMessagePerformed.conversationMessage should be(defined)
        val message = createMessagePerformed.conversationMessage.value
        message.content shouldBe "test message"
        message.id should not be null
      }
    }

    "be able to delete conversation message (DELETE /conversation/messages/<uuid>)" in withCleanDatabase {
      val conversationId = UUID.random
      val convMessIds = Seq(
        "test-conversation-message1",
        "test-conversation-message2",
        "test-conversation-message3"
      ).map { convMessTitle =>
        val conversationMessageRow = ConversationMessageRow(UUID.random, conversationId, Role.USER, convMessTitle)
        conversationMessageDb.insert(conversationMessageRow).futureValue
        conversationMessageRow.id
      }

      val convMessIdsToDelete = convMessIds.head
      Delete(s"/conversation/messages/$convMessIdsToDelete") ~> routes ~> check {
        val responseMessage = responseAs[ConversationMessageActionPerformed]
        responseMessage.status should be(ConversationActionPerfomedStatus.DONE)
        responseMessage.message shouldBe s"Conversation message $convMessIdsToDelete deleted."
      }

      conversationMessageDb.getAllMessages.futureValue.size shouldBe convMessIds.size - 1
    }
  }

  "get an error if uuid format is wrong while delete conversation message (DELETE /conversation/messages/<uuid>)" in {
    val deleteRequest = Delete("/conversation/messages/invalid-uuid")
    deleteRequest ~> routes ~> check {
      status should be(StatusCodes.BadRequest)
      responseAs[String] should be("Invalid UUID format")
    }
  }
}

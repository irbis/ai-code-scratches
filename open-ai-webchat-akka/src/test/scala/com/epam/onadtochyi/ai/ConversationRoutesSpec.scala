package com.epam.onadtochyi.ai

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.epam.onadtochyi.ai.task.AiJsonFormats._
import com.epam.onadtochyi.ai.task.ConversationRoutes
import com.epam.onadtochyi.ai.task.registry.ConversationActionPerfomedStatus.DONE
import com.epam.onadtochyi.ai.task.registry.ConversationRegistry.{ConversationActionPerformed, GetConversationsResponse}
import com.epam.onadtochyi.ai.task.registry.{ConversationDatabase, ConversationRegistry, ConversationsTable}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.jdbc.HsqldbProfile.api._

import scala.concurrent.duration.DurationInt

class ConversationRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  private lazy val testKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[_] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val db = Database.forURL(
    url  = "jdbc:hsqldb:mem:aitestdb;shutdown=false",
    driver = "org.hsqldb.jdbcDriver"
  )

  val conversationDb = new ConversationDatabase(db)

  private val conversationRegistry = testKit.spawn(ConversationRegistry(conversationDb))
  private lazy val routes = new ConversationRoutes(conversationRegistry).conversationRoutes

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import io.jvm.uuid._
  import org.scalatest.OptionValues._

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  override def beforeAll(): Unit = {
    super.beforeAll()
    conversationDb.createSchema().futureValue
  }

  override def afterAll(): Unit = {
    db.close()
    super.afterAll()
  }

  def withCleanDatabase[T](testCode: => T): T = {
    try {
      testCode
    } finally {
      db.run(TableQuery[ConversationsTable].delete).futureValue
    }
  }
  
  "ConversationRoutes" should {
    "return no users if no present (GET /conversations)" in {
      val request = HttpRequest(uri = "/conversations")

      request ~> routes ~> check {
        status shouldBe StatusCodes.OK

        contentType shouldBe ContentTypes.`application/json`
        entityAs[String] should ===("""{"conversations":[]}""")
      }
    }

    "be able to add convesation (POST /conversations)" in withCleanDatabase {
      val request = Post("/conversation/add-new-conversation")

      request ~> routes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`

        val actionPerformed = responseAs[ConversationActionPerformed]
        actionPerformed.status shouldBe DONE
        actionPerformed.message shouldBe "Conversation add-new-conversation created."
        actionPerformed.conversation shouldBe defined
        actionPerformed.conversation.get.title shouldBe "add-new-conversation"
      }
    }

    "be able to add several convesations (POST /conversations)" in withCleanDatabase {
      val convIds = Seq(
        "add-several-conversations1",
        "add-several-conversations2",
        "add-several-conversations3"
      ).map { convTitle =>
        Post("/conversation/" + convTitle) ~> routes ~> check {
          responseAs[ConversationActionPerformed].conversation.get.id }
      }

      Get("/conversations") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val responseConversation = responseAs[GetConversationsResponse]
        responseConversation.conversations.size shouldBe convIds.size
        convIds.foreach { convId =>
          responseConversation.conversations.exists(_.id == convId) shouldBe true
        }
      }
    }

    "be able to get conversation by uuid (GET /conversations/<uuid>)" in withCleanDatabase {
      val postRequest = Post("/conversation/get-conversation-by-uuid")

      postRequest ~> routes ~> check {
        status shouldBe StatusCodes.Created
        val createConvActionPerformed = responseAs[ConversationActionPerformed]

        val getRequest = Get(s"/conversations/${createConvActionPerformed.conversation.get.id}")
        getRequest ~> routes ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          val responseConversation = responseAs[ConversationActionPerformed]
          responseConversation.status shouldBe DONE
          responseConversation.conversation.value shouldBe createConvActionPerformed.conversation.value
        }
      }
    }

    "get error if uuid format is wrong (GET /conversations/<uuid>)" in {
      val getRequest = Get("/conversations/invalid-uuid")
      getRequest ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe "Invalid UUID format"
      }
    }

    "while getting a conversation get an empty response if conversation not found by uuid (GET /conversations/<uuid>)" in {
      val getRequest = Get(s"/conversations/${UUID.random}")
      getRequest ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`

        val responseConversation = responseAs[ConversationActionPerformed]
        responseConversation.conversation shouldBe None
      }
    }

    "be able to delete conversation (DELETE /conversations)" in {
      val convIds = Seq(
        "test-conversation1",
        "test-conversation2",
        "test-conversation3"
      ).map { convTitle =>
          Post("/conversation/" + convTitle) ~> routes ~> check { responseAs[ConversationActionPerformed].conversation.get.id }
      }

      val convIdsToDelete = convIds.head
      Delete(s"/conversations/$convIdsToDelete") ~> routes ~> check {
        val responseConversation = responseAs[ConversationActionPerformed]
        responseConversation.status shouldBe DONE
        responseConversation.message shouldBe s"Conversation $convIdsToDelete deleted."
        responseConversation.conversation shouldBe None
      }

      Get(s"/conversations/$convIdsToDelete") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val responseConversation = responseAs[ConversationActionPerformed]
        responseConversation.status shouldBe DONE
        responseConversation.conversation shouldBe None
      }

      Get("/conversations") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`application/json`
        val responseConversation = responseAs[GetConversationsResponse]
        responseConversation.conversations.size shouldBe convIds.size - 1
      }
    }

    "while delete conversation get an error if uuid format is wrong (DELETE /conversations/<uuid>)" in {
      val deleteRequest = Delete("/conversations/invalid-uuid")
      deleteRequest ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe "Invalid UUID format"
      }
    }
  }
}

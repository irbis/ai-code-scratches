package com.epam.onadtochyi.ai

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model._
import com.epam.onadtochyi.ai.task.AiJsonFormats._
import com.epam.onadtochyi.ai.task.ConversationRoutes
import com.epam.onadtochyi.ai.task.dto.Conversation
import com.epam.onadtochyi.ai.task.registry.ConversationActionPerfomedStatus.DONE
import com.epam.onadtochyi.ai.task.registry.ConversationRegistry
import com.epam.onadtochyi.ai.task.registry.ConversationRegistry.{AiActionPerformed, ConversationActionPerformed, GetConversationsResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConversationRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  lazy val testKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[_] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val conversationRegistry = testKit.spawn(ConversationRegistry())
  lazy val routes = new ConversationRoutes(conversationRegistry).conversationRoutes

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import io.jvm.uuid._
  import org.scalatest.OptionValues._

  "ConversationRoutes" should {
    "return no users if no present (GET /conversations)" in {
      val request = HttpRequest(uri = "/conversations")

      request ~> routes ~> check {
        status shouldBe StatusCodes.OK

        contentType shouldBe ContentTypes.`application/json`
        entityAs[String] should ===("""{"conversations":[]}""")
      }
    }

    "be able to add convestations (POST /conversations)" in {
      val request = Post("/conversation/test-conversation")

      request ~> routes ~> check {
        status shouldBe StatusCodes.Created
        contentType shouldBe ContentTypes.`application/json`

        val conversation = responseAs[Conversation]
        conversation.id.toString should not be empty
        conversation.title should not be empty
        conversation.title shouldBe "test-conversation"
      }
    }

    "be able to get conversation by uuid (GET /conversations/<uuid>)" in {
      val postRequest = Post("/conversation/test-conversation")

      postRequest ~> routes ~> check {
        status shouldBe StatusCodes.Created
        val conversation = responseAs[Conversation]

        val getRequest = Get(s"/conversations/${conversation.id}")
        getRequest ~> routes ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          val responseConversation = responseAs[ConversationActionPerformed]
          responseConversation.status shouldBe DONE
          responseConversation.conversation.value shouldBe conversation
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

        val responseConversation = responseAs[GetConversationsResponse]
        responseConversation.maybeConversation shouldBe None
      }
    }

    "be able to delete conversation (DELETE /conversations)" in {
      val postRequest = Post("/conversation/test-conversation")

      postRequest ~> routes ~> check {
        status shouldBe StatusCodes.Created
        val conversation = responseAs[Conversation]

        val deleteRequest = Delete(s"/conversations/${conversation.id}")
        deleteRequest ~> routes ~> check {
          val responseConversation = responseAs[ConversationActionPerformed]
          responseConversation.status shouldBe DONE
          responseConversation.message shouldBe s"Conversation ${conversation.id} deleted."
          responseConversation.conversation shouldBe None
        }
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

package com.epam.onadtochyi.ai.task

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.epam.onadtochyi.ai.task.conversationmessage.ConversationMessageRepositoryActor
import com.epam.onadtochyi.ai.task.conversationmessage.ConversationMessageRepositoryActor.{ConversationMessageActionPerformed, CreateConversationMessageRequest}

import scala.concurrent.Future

class ConversationMessageRoutes (
                                conversationMessageRepositoryActor: ActorRef[ConversationMessageRepositoryActor.Command]
                                )
                                (
                                implicit val system: ActorSystem[_]
                                )
{
  import AiJsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("ai-web-chat-app.routes.ask-timeout"))
  
  private def createConversationMessage(createConversationMessageRequest: CreateConversationMessageRequest): Future[ConversationMessageActionPerformed] =
    conversationMessageRepositoryActor.ask(ConversationMessageRepositoryActor.CreateConversationMessage(createConversationMessageRequest, _))
    
  private val createConversationMessageRoute: Route = {
    path("conversation" / "message") {
      post {
        entity(as[CreateConversationMessageRequest]) { request =>
          onSuccess(createConversationMessage(request)) { performed =>
            complete((StatusCodes.Created, performed))
          }
        }
      }
    }
  }
}

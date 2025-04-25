package com.epam.onadtochyi.ai.task

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.epam.onadtochyi.ai.task.registry.{ConversationRegistry, Conversations}

import scala.concurrent.Future
import ConversationRegistry._
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.epam.onadtochyi.ai.task.dto.Conversation
import com.epam.onadtochyi.ai.task.registry.ConversationActionPerfomedStatus._

class ConversationRoutes (
                         conversationRegistry: ActorRef[ConversationRegistry.Command]
                         )
                         (
                           implicit val system: ActorSystem[_]
                         )
{
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import AiJsonFormats._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getConversations(): Future[Conversations] =
    conversationRegistry.ask(GetConversations.apply)
  def getConversation(id: String): Future[ConversationActionPerformed] =
    conversationRegistry.ask(GetConversation(id, _))
  def createConversation(title: String): Future[Conversation] =
    conversationRegistry.ask(CreateConversation(title, _))
  def deleteConversation(id: String): Future[ConversationActionPerformed] =
    conversationRegistry.ask(DeleteConversation(id, _))

  private def conversationActionPerformedToRoute(performed: ConversationActionPerformed) =
    performed.status match {
      case DONE => complete(StatusCodes.OK, performed)
      case ERR => complete(StatusCodes.BadRequest, performed.message)
    }

  private val createConversationRoute: Route = {
    pathPrefix("conversation") {
      path(Segment) { title =>
        post {
          onSuccess(createConversation(title)) { performed =>
            complete(StatusCodes.Created, performed)
          }
        }
      }
    }
  }

  private val restRoutes: Route =
    pathPrefix("conversations") {
      concat(
        pathEnd {
          concat(
            get {
              complete(getConversations())
            }
          )
        },
        path(Segment) { id =>
          concat(
            get {
              onSuccess(getConversation(id)) { performed =>
                conversationActionPerformedToRoute(performed)
              }
            },
            delete {
              onSuccess(deleteConversation(id)) { performed =>
                conversationActionPerformedToRoute(performed)
              }
            }
          )
        }
      )
    }

  val conversationRoutes: Route = createConversationRoute ~ restRoutes
}

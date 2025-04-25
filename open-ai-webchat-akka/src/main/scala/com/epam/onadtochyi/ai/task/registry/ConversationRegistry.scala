package com.epam.onadtochyi.ai.task.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.epam.onadtochyi.ai.task.dto.{Conversation, Message}
import com.epam.onadtochyi.ai.task.registry.ConversationActionPerfomedStatus.{ConversationActionPerfomedStatus, DONE, ERR}

import scala.collection.immutable
import scala.util.{Failure, Success, Try}
import io.jvm.uuid._

final case class Conversations(conversations: immutable.Seq[Conversation])

object ConversationRegistry {

  sealed trait Command
  final case class GetConversations(replyTo: ActorRef[Conversations]) extends Command
  final case class CreateConversation(title: String, replyTo: ActorRef[Conversation]) extends Command
  final case class GetConversation(id: String, replyTo: ActorRef[ConversationActionPerformed]) extends Command
  final case class DeleteConversation(id: String, replyTo: ActorRef[ConversationActionPerformed]) extends Command

  //final case class CreateConversationRequest()
  final case class GetConversationsResponse(maybeConversation: Option[Conversation])
  final case class AiActionPerformed(description: String)
  final case class ConversationActionPerformed(
                                                status: ConversationActionPerfomedStatus,
                                                message: String = "",
                                                conversation: Option[Conversation] = Option.empty
                                              )
  object ConversationActionPerformed {
    final val ERR_INVALID_UUID_FORMAT = apply(ERR, "Invalid UUID format")
  }

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(conversations: Set[Conversation]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetConversations(replyTo) =>
        replyTo ! Conversations(conversations.toSeq)
        Behaviors.same
      case CreateConversation(title, replyTo) =>
        val newConversation = Conversation(title = title)
        replyTo ! newConversation
        registry(conversations + newConversation)
      case GetConversation(id, replyTo) =>
          Try(UUID.fromString(id)) match {
          case Success(id) => replyTo ! ConversationActionPerformed(DONE, conversation = conversations.find(_.id == id))
          case Failure(_) => replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
        }
        Behaviors.same
      case DeleteConversation(id, replyTo) =>
        Try(UUID.fromString(id)) match {
          case Success(id) =>
            replyTo ! ConversationActionPerformed(DONE, s"Conversation $id deleted.")
            registry(conversations.filterNot(_.id == id))
          case Failure(_) =>
            replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
            Behaviors.same
        }
    }
}

package com.epam.onadtochyi.ai.task.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.epam.onadtochyi.ai.task.dto.{Conversation, Message}
import com.epam.onadtochyi.ai.task.registry.ConversationActionPerfomedStatus.{ConversationActionPerfomedStatus, DONE, ERR}

import scala.collection.immutable
import scala.util.{Failure, Success, Try}
import io.jvm.uuid._

object ConversationRegistry {

  sealed trait Command
  final case class GetConversations(replyTo: ActorRef[GetConversationsResponse]) extends Command
  final case class CreateConversation(title: String, replyTo: ActorRef[Conversation]) extends Command
  final case class GetConversation(id: String, replyTo: ActorRef[ConversationActionPerformed]) extends Command
  final case class DeleteConversation(id: String, replyTo: ActorRef[ConversationActionPerformed]) extends Command

  final case class GetConversationsResponse(conversations: immutable.Seq[Conversation])
  final case class AiActionPerformed(description: String)
  final case class ConversationActionPerformed(
                                                status: ConversationActionPerfomedStatus,
                                                message: String = "",
                                                conversation: Option[Conversation] = Option.empty
                                              )
  object ConversationActionPerformed {
    final val ERR_INVALID_UUID_FORMAT = apply(ERR, "Invalid UUID format")
  }

  val conversationRepository = new ConversationRepository()

  def apply(): Behavior[Command] = registry()

  private def registry(): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetConversations(replyTo) =>
        val convs = GetConversationsResponse(conversationRepository.getAll)
        replyTo ! convs
        Behaviors.same
      case CreateConversation(title, replyTo) =>
        val newConversation = Conversation(title = title)
        replyTo ! conversationRepository.create(newConversation)
        Behaviors.same
      case GetConversation(id, replyTo) =>
          Try(UUID.fromString(id)) match {
          case Success(id) => replyTo ! ConversationActionPerformed(DONE, conversation = conversationRepository.getById(id))
          case Failure(_) => replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
        }
        Behaviors.same
      case DeleteConversation(id, replyTo) =>
        Try(UUID.fromString(id)) match {
          case Success(id) =>
            replyTo ! ConversationActionPerformed(DONE, s"Conversation $id deleted.")
            conversationRepository.delete(id)
          case Failure(_) =>
            replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
        }
        Behaviors.same
    }
}

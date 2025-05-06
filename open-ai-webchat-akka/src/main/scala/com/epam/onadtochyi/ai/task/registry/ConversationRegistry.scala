package com.epam.onadtochyi.ai.task.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.epam.onadtochyi.ai.task.dto.Conversation
import com.epam.onadtochyi.ai.task.registry.ConversationActionPerfomedStatus.{ConversationActionPerfomedStatus, DONE, ERR}
import io.jvm.uuid._

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

object ConversationRegistry {

  sealed trait Command
  final case class GetConversations(replyTo: ActorRef[GetConversationsResponse]) extends Command
  final case class CreateConversation(title: String, replyTo: ActorRef[ConversationActionPerformed]) extends Command
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

    def apply(conversation:Option[Conversation]): ConversationActionPerformed =
      ConversationActionPerformed(DONE, conversation = conversation)
  }

  private final case class WrappedGetConversationsResponse(
                                                      convRows: Seq[Conversation],
                                                      replyTo: ActorRef[GetConversationsResponse]
                                                    ) extends Command

  private final case class WrappedConversationActionPerformed(
                                                      conversationAction: ConversationActionPerformed,
                                                      replyTo: ActorRef[ConversationActionPerformed]
                                                      ) extends Command

  private object WrappedConversationActionPerformed {
    def apply(conversation: Option[Conversation], replyTo: ActorRef[ConversationActionPerformed]): WrappedConversationActionPerformed =
      WrappedConversationActionPerformed(
        ConversationActionPerformed(conversation),
        replyTo)

    def apply(message: String, replyTo: ActorRef[ConversationActionPerformed]): WrappedConversationActionPerformed =
      WrappedConversationActionPerformed(
        ConversationActionPerformed(DONE, message),
        replyTo)

    def apply(exception: Throwable, replyTo: ActorRef[ConversationActionPerformed]): WrappedConversationActionPerformed =
      WrappedConversationActionPerformed(
        ConversationActionPerformed(ERR, s"Database operation error: ${exception.getMessage}"),
        replyTo)
  }

  private def toConversation(cRow: ConversationRow) = Conversation(cRow.id, cRow.title)
  private def toConversationRow(c: Conversation) = ConversationRow(c.id, c.title)

  def apply(conversationDatabase: ConversationDatabase): Behavior[Command] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case GetConversations(replyTo) =>
        context.pipeToSelf(conversationDatabase.getAllConversations) {
          case Success(convRows) => WrappedGetConversationsResponse(convRows.map(toConversation), replyTo)
          case Failure(_) => WrappedGetConversationsResponse(Seq.empty, replyTo)
        }
        Behaviors.same
      case WrappedGetConversationsResponse(conversations, replyTo) =>
        replyTo ! GetConversationsResponse(conversations)
        Behaviors.same

      case CreateConversation(title, replyTo) =>
        val newConversation = Conversation(title = title)
        context.pipeToSelf(conversationDatabase.insert(toConversationRow(newConversation))) {
          case Success(_) => WrappedConversationActionPerformed(ConversationActionPerformed(
            DONE, s"Conversation ${newConversation.title} created.", Option(newConversation)), replyTo)
          case Failure(ex) => WrappedConversationActionPerformed(ex, replyTo)
        }
        Behaviors.same

      case GetConversation(id, replyTo) =>
        Try(UUID.fromString(id)) match {
          case Success(id) =>
            context.pipeToSelf(conversationDatabase.getById(id)) {
              case Success(conversationRow) => WrappedConversationActionPerformed(conversationRow.map(toConversation), replyTo)
              case Failure(ex) => WrappedConversationActionPerformed(ex, replyTo)
            }
          case Failure(_) => replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
        }
        Behaviors.same

      case DeleteConversation(id, replyTo) =>
        Try(UUID.fromString(id)) match {
          case Success(id) =>
            context.pipeToSelf(conversationDatabase.deleteConversation(id)) {
              case Success(_) => WrappedConversationActionPerformed(s"Conversation $id deleted.", replyTo)
              case Failure(ex) => WrappedConversationActionPerformed(ex, replyTo)
            }
            Behaviors.same
          case Failure(_) => replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
            Behaviors.same
        }

      case WrappedConversationActionPerformed(conversationActionPerformed, replyTo) =>
        replyTo ! conversationActionPerformed
        Behaviors.same
    }
  }


}

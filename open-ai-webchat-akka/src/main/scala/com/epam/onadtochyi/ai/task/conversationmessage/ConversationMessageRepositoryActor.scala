package com.epam.onadtochyi.ai.task.conversationmessage

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.epam.onadtochyi.ai.task.conversation.ConversationActionPerfomedStatus
import com.epam.onadtochyi.ai.task.conversation.ConversationActionPerfomedStatus.ConversationActionPerfomedStatus
import com.epam.onadtochyi.ai.task.dto.Message
import com.epam.onadtochyi.ai.task.dto.Role.Role
import io.jvm.uuid._

import scala.util.{Failure, Success, Try}

object ConversationMessageRepositoryActor {

  sealed trait Command

  final case class CreateConversationMessageRequest(conversationId: UUID, message: String, role: Role)
  final case class CreateConversationMessage(
                                              createConversationMessageRequest: CreateConversationMessageRequest,
                                              replyTo: ActorRef[ConversationMessageActionPerformed]) extends Command
  final case class GetConversationMessage(id: String, replyTo: ActorRef[ConversationMessageActionPerformed]) extends Command
  final case class DeleteConversationMessage(id: String, replyTo: ActorRef[ConversationMessageActionPerformed]) extends Command

  final case class ConversationMessageActionPerformed(
                                                     status: ConversationActionPerfomedStatus,
                                                     message: String = "",
                                                     conversationMessage: Option[Message] = Option.empty
                                                     )

  final case class WrappedConversationMessageActionPerformed(
                                                              conversationMessageActionPerformed: ConversationMessageActionPerformed,
                                                              replyTo: ActorRef[ConversationMessageActionPerformed]
                                                            ) extends Command

  private def toMessage(c: ConversationMessageRow): Message = Message(c.id, c.role, c.content)

  def apply(conversationMessageDatabase: ConversationMessageDatabase): Behavior[Command] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case CreateConversationMessage(r: CreateConversationMessageRequest, replyTo) =>
        val newMessageId = UUID.random
        val conversationMessageRow = ConversationMessageRow(newMessageId, r.conversationId, r.role, r.message)
        context.pipeToSelf(conversationMessageDatabase.insert(conversationMessageRow)) {
          case Success(_) => WrappedConversationMessageActionPerformed(
            ConversationMessageActionPerformed(
              ConversationActionPerfomedStatus.DONE, s"Message $newMessageId created.",
              Option(Message(newMessageId, r.role, r.message))
            ),
            replyTo
          )
        }
        Behaviors.same

      case GetConversationMessage(id, replyTo) =>
        Try(UUID.fromString(id)) match {
          case Success(id) =>
            context.pipeToSelf(conversationMessageDatabase.getById(id)) {
              case Success(conversationMessageRow) =>
                WrappedConversationMessageActionPerformed(
                  ConversationMessageActionPerformed(
                    ConversationActionPerfomedStatus.DONE, "",
                    conversationMessageRow.map(toMessage),
                  ), replyTo
                )
              case Failure(ex) => WrappedConversationMessageActionPerformed(
                ConversationMessageActionPerformed(ConversationActionPerfomedStatus.ERR,
                  s"Database operation error: ${ex.getMessage}"),
                replyTo)
            }
          case Failure(_) => replyTo ! ConversationMessageActionPerformed(ConversationActionPerfomedStatus.ERR, "Invalid UUID format")
        }
        Behaviors.same

      case DeleteConversationMessage(id, replyTo) =>
        Try(UUID.fromString(id)) match {
          case Success(id) =>
            context.pipeToSelf(conversationMessageDatabase.deleteMessage(id)) {
              case Success(_) => WrappedConversationMessageActionPerformed(
                ConversationMessageActionPerformed(ConversationActionPerfomedStatus.DONE,
                  s"Conversation message $id deleted."),
                replyTo
              )
              case Failure(ex) => WrappedConversationMessageActionPerformed(
                ConversationMessageActionPerformed(ConversationActionPerfomedStatus.ERR,
                  s"Database operation error: ${ex.getMessage}"),
                replyTo)
            }
          case Failure(_) => replyTo ! ConversationMessageActionPerformed(ConversationActionPerfomedStatus.ERR, "Invalid UUID format")
        }
        Behaviors.same

      case WrappedConversationMessageActionPerformed(conversationMessageActionPerformed, replyTo) =>
        replyTo ! conversationMessageActionPerformed
        Behaviors.same
    }
  }
}

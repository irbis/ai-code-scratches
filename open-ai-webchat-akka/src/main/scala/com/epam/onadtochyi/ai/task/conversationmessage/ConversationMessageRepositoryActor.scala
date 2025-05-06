package com.epam.onadtochyi.ai.task.conversationmessage

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.epam.onadtochyi.ai.task.conversation.ConversationActionPerfomedStatus
import com.epam.onadtochyi.ai.task.conversation.ConversationActionPerfomedStatus.ConversationActionPerfomedStatus
import com.epam.onadtochyi.ai.task.dto.Message
import com.epam.onadtochyi.ai.task.dto.Role.Role
import io.jvm.uuid._

import scala.util.Success

object ConversationMessageRepositoryActor {

  sealed trait Command

  final case class CreateConversationMessageRequest(conversationId: UUID, message: String, role: Role)
  final case class CreateConversationMessage(
                                              createConversationMessageRequest: CreateConversationMessageRequest,
                                              replyTo: ActorRef[ConversationMessageActionPerformed]) extends Command

  final case class ConversationMessageActionPerformed(
                                                     status: ConversationActionPerfomedStatus,
                                                     message: String = "",
                                                     conversationMessage: Option[Message] = Option.empty
                                                     )

  final case class WrappedConversationMessageActionPerformed(
                                                              conversationMessageActionPerformed: ConversationMessageActionPerformed,
                                                              replyTo: ActorRef[ConversationMessageActionPerformed]
                                                            ) extends Command

  def apply(conversationMessageDatabase: ConversationMessageDatabase): Behavior[Command] = Behaviors.setup{ context =>

    Behaviors.receiveMessage {
      case CreateConversationMessage(r: CreateConversationMessageRequest, replyTo) =>
        val newMessageId = UUID.random
        val conversationMessageRow = ConversationMessageRow(newMessageId, r.conversationId, r.message, r.role)
        context.pipeToSelf(conversationMessageDatabase.insert(conversationMessageRow)) {
          case Success(_) => WrappedConversationMessageActionPerformed(
            ConversationMessageActionPerformed(ConversationActionPerfomedStatus.DONE, s"Message $newMessageId created."),
            replyTo
          )
        }
        Behaviors.same

      case WrappedConversationMessageActionPerformed(conversationMessageActionPerformed, replyTo) =>
        replyTo ! conversationMessageActionPerformed
        Behaviors.same
    }
  }
}

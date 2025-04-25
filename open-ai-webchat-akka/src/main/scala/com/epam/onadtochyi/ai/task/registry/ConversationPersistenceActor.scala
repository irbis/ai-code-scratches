package com.epam.onadtochyi.ai.task.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.epam.onadtochyi.ai.task.dto.Conversation
import com.epam.onadtochyi.ai.task.registry.ConversationActionPerfomedStatus.{ConversationActionPerfomedStatus, DONE, ERR}
import com.epam.onadtochyi.ai.task.registry.ConversationRegistry.ConversationActionPerformed

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import io.jvm.uuid._

object ConversationPersistenceActor {
  sealed trait Command
  final case class SaveConversation(conversation: Conversation, replyTo: ActorRef[ConversationActionPerformed]) extends Command
  final case class LoadConversation(id: UUID, replyTo: ActorRef[ConversationActionPerformed]) extends Command
  final case class LoadAllConversations(replyTo: ActorRef[Conversations]) extends Command
  final case class DeleteConversationFromDb(id: UUID, replyTo: ActorRef[ConversationActionPerformed]) extends Command

  def apply(database: ConversationDatabase): Behavior[Command] = Behaviors.setup { context =>
    implicit val ec: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case SaveConversation(conversation, replyTo) =>
        context.pipeToSelf(database.insert(ConversationRow(conversation.id, conversation.title))) {
          case Success(_) =>
            SaveConversation(conversation, replyTo)
          case Failure(_) =>
            SaveConversation(conversation, replyTo)
        }

        replyTo ! ConversationActionPerformed(DONE, conversation = Some(conversation))
        Behaviors.same

      case LoadConversation(id, replyTo) =>
        context.pipeToSelf(database.getById(id)) {
          case Success(maybeConv) =>
            maybeConv match {
              case Some(conv) => replyTo ! ConversationActionPerformed(DONE, conversation = Some(Conversation(conv.id, conv.title)))
              case None => replyTo ! ConversationActionPerformed(ERR, s"Conversation with id $id not found")
            }
          case Failure(ex) =>
            replyTo ! ConversationActionPerformed(ERR, s"Error loading conversation: ${ex.getMessage}")
        }
        Behaviors.same

      case LoadAllConversations(replyTo) =>
        context.pipeToSelf(database.getAllConversations()) {
          case Success(convs) => replyTo ! Conversations(convs)
          case Failure(ex) => replyTo ! Conversations(Seq.empty)
        }
        Behaviors.same

      case DeleteConversationFromDb(id, replyTo) =>
        context.pipeToSelf(database.deleteConversation(id)) {
          case Success(_) =>
            replyTo ! ConversationActionPerformed(DONE, s"Conversation $id deleted from database")
          case Failure(ex) =>
            replyTo ! ConversationActionPerformed(ERR, s"Error deleting conversation: ${ex.getMessage}")
        }
        Behaviors.same
    }
  }
}

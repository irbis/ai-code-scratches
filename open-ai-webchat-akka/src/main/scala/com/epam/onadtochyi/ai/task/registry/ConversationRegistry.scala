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
      ConversationActionPerformed(DONE, "", conversation)
  }

  private final case class WrappedGetConversationsResponse(
                                                      convRows: Seq[ConversationRow],
                                                      replyTo: ActorRef[GetConversationsResponse]
                                                    ) extends Command

  private final case class WrappedConversationActionPerformed(
                                                      conversationAction: ConversationActionPerformed,
                                                      replyTo: ActorRef[ConversationActionPerformed]
                                                      ) extends Command
  private object WrappedConversationActionPerformed {
    def apply(conversationRow: Option[ConversationRow], replyTo: ActorRef[ConversationActionPerformed]): WrappedConversationActionPerformed =
      WrappedConversationActionPerformed(
        ConversationActionPerformed(conversationRow.map(toConversation)),
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

  private final val conversationRepository = new ConversationRepository()

  def apply(conversationDatabase: ConversationDatabase): Behavior[Command] = Behaviors.setup { context =>


    Behaviors.receiveMessage {
      case GetConversations(replyTo) =>
        context.pipeToSelf(conversationDatabase.getAllConversations) {
          case Success(convRows) => WrappedGetConversationsResponse(convRows, replyTo)
          case Failure(_) => WrappedGetConversationsResponse(Seq.empty, replyTo)
        }
        Behaviors.same
      case WrappedGetConversationsResponse(convRows, replyTo) =>
        replyTo ! GetConversationsResponse(convRows.map(toConversation))
        Behaviors.same

//        conversationDatabase.getAllConversations.onComplete { tryConvs =>
//          tryConvs match {
//            case Success(value) =>
//              val convs = value.map(toConversation)
//              replyTo ! GetConversationsResponse(convs)
//            case Failure(ex) =>
//              replyTo ! GetConversationsResponse(Seq.empty)
//          }
//        }
//        Behaviors.same

      case CreateConversation(title, replyTo) =>
        val newConversation = Conversation(title = title)
        context.pipeToSelf(conversationDatabase.insert(toConversationRow(newConversation))) {
          case Success(_) => WrappedConversationActionPerformed(ConversationActionPerformed(DONE, conversation = Option(newConversation)), replyTo)
          case Failure(ex) => WrappedConversationActionPerformed(ex, replyTo)
        }
        Behaviors.same
//        conversationDatabase.insert(toConversationRow(newConversation)).onComplete { status => // TODO what is status?
//          replyTo ! newConversation
//        }
//        Behaviors.same

      case GetConversation(id, replyTo) =>
        Try(UUID.fromString(id)) match {
          case Success(id) =>
            context.pipeToSelf(conversationDatabase.getById(id)) {
              case Success(conversationRow) => WrappedConversationActionPerformed(conversationRow, replyTo)
              case Failure(ex) => WrappedConversationActionPerformed(ex, replyTo)
            }
          case Failure(_) => replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
        }
        Behaviors.same

//            conversationDatabase.getById(id).onComplete { tryConv =>
//              tryConv match {
//                case Success(mayBeConvRow) => replyTo ! ConversationActionPerformed(DONE,
//                  conversation = mayBeConvRow.map(toConversation))
//                case Failure(_) => replyTo ! ConversationActionPerformed(DONE) // TODO exception handling
//              }
//            }
//          case Failure(_) => replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
//        }
//        Behaviors.same

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

//      case DeleteConversation(id, replyTo) =>
//        Try(UUID.fromString(id)) match {
//          case Success(id) =>
//            conversationDatabase.deleteConversation(id).onComplete { status => // TODO what is status?
//              replyTo ! ConversationActionPerformed(DONE, s"Conversation $id deleted.")
//            }
//          case Failure(_) => replyTo ! ConversationActionPerformed.ERR_INVALID_UUID_FORMAT
//        }
//        Behaviors.same
    }
  }

  private def toConversation(cRow: ConversationRow) = Conversation(cRow.id, cRow.title)
  private def toConversationRow(c: Conversation) = ConversationRow(c.id, c.title)

  private def registry(): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetConversations(replyTo) =>
        val convs = GetConversationsResponse(conversationRepository.getAll)
        replyTo ! convs
        Behaviors.same
//      case CreateConversation(title, replyTo) =>
//        val newConversation = Conversation(title = title)
//        replyTo ! conversationRepository.create(newConversation)
//        Behaviors.same
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

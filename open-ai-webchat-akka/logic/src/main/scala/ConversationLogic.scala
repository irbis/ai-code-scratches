package com.epam.onadtochyi.ai.task.logic

import com.epam.onadtochyi.ai.task.domain.model.Conversation
import com.epam.onadtochyi.ai.task.repositoryinterface.repository.ConversationRepository
import com.epam.onadtochyi.ai.task.services.ConversationService
import io.jvm.uuid._

class ConversationLogic (val conversationRepository: ConversationRepository)
  extends ConversationService {

  override def createConversation(title: String): Conversation = {
    val conversation = Conversation(title = title)
    conversationRepository.createConversation(conversation)
    conversation
  }

  override def getAllConversations: List[Conversation] =
    conversationRepository.getAllConversations.toList

  override def createConversation(conversation: Conversation): Unit =
    conversationRepository.createConversation(conversation)

  override def getConversation(uuid: UUID): Option[Conversation] =
    conversationRepository.getConversation(uuid)

  override def updateConversation(conversation: Conversation): Unit =
    conversationRepository.updateConversation(conversation)

  override def deleteConversation(conversation: Conversation): Unit =
  conversationRepository.deleteConversation(conversation.id)

  override def deleteConversation(uuid: UUID): Unit =
  conversationRepository.deleteConversation(uuid)
}

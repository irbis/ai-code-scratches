package com.epam.onadtochyi.ai.task

import com.epam.onadtochyi.ai.task.domain.model.Conversation
import com.epam.onadtochyi.ai.task.repositoryinterface.repository.ConversationRepository

import java.util.UUID
import scala.collection.mutable.ListBuffer

object ConversationRepositoryListImpl extends ConversationRepository {
  private val conversations = ListBuffer[Conversation]()

  override def getConversation(UUID: UUID): Option[Conversation] = 
    conversations.find(_.id == UUID)

  override def getAllConversations: Seq[Conversation] = 
    conversations.toSeq

  override def createConversation(conversation: Conversation): UUID = {
    conversations += conversation
    conversation.id
  }

  override def deleteConversation(UUID: UUID): Unit = 
    conversations.filterInPlace(_.id != UUID)

  override def updateConversation(conversation: Conversation): Unit = {
    val index = conversations.indexWhere(_.id == conversation.id)
    if (index >= 0) conversations(index) = conversation
  }
}

package com.epam.onadtochyi.ai.task.repositoryinterface.repository

import com.epam.onadtochyi.ai.task.domain.model.Conversation

import java.util.UUID

trait ConversationRepository {
  def getConversation(UUID: UUID): Option[Conversation]
  def getAllConversations: Seq[Conversation]
  def createConversation(conversation: Conversation): UUID
  def deleteConversation(UUID: UUID): Unit
  def updateConversation(conversation: Conversation): Unit
}

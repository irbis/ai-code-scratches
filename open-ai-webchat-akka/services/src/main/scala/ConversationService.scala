package com.epam.onadtochyi.ai.task.services

import com.epam.onadtochyi.ai.task.domain.model.Conversation
import io.jvm.uuid._

trait ConversationService {
  def createConversation(title: String): Conversation
  def createConversation(conversation: Conversation)
  def getConversation(uuid: UUID) : Option[Conversation]
  def updateConversation(conversation: Conversation)
  def deleteConversation(conversation: Conversation)
  def deleteConversation(uuid: UUID)
}

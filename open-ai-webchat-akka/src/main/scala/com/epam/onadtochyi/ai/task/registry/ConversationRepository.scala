package com.epam.onadtochyi.ai.task.registry

import com.epam.onadtochyi.ai.task.dto.Conversation

import scala.collection.{mutable, immutable}
import io.jvm.uuid._

class ConversationRepository {
  private val conversations = mutable.ListBuffer[Conversation]()

  def getAll: immutable.Seq[Conversation] = conversations.toSeq

  def getById(id: UUID): Option[Conversation] = conversations.find(_.id == id)

  def create(conversation: Conversation): Conversation = {
    conversations.append(conversation)
    conversation
  }

  def delete(id: UUID): Unit = {
    conversations.filterInPlace(_.id != id)
  }

}

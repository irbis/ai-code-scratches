package com.epam.onadtochyi.ai
package task.dto

import java.util.UUID

case class Conversation (
                          id: UUID = UUID.randomUUID(),
                          messages: List[Message] = List()
                        )
{

  def addMessage(message: Message): Conversation = Conversation(id, messages :+ message)

}

object Conversation {

  def apply(message: Message): Conversation = new Conversation(messages = message :: Nil)

}

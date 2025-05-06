package com.epam.onadtochyi.ai.task.dto

import com.epam.onadtochyi.ai.task.dto.Role.Role
import io.jvm.uuid._

case class Message(id: UUID = UUID.random, role: Role, content: String)

object Message {
  def apply(role: Role, content: String): Message = Message(role = role, content = content)
}
package com.epam.onadtochyi.ai.task.dto

import com.epam.onadtochyi.ai.task.dto.Role.Role
import io.jvm.uuid._

case class Message(role: Role, content: String, id: UUID = UUID.random)

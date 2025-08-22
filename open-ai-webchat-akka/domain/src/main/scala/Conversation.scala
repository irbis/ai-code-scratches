package com.epam.onadtochyi.ai.task.domain.model

import io.jvm.uuid._

final case class Conversation (
                                id:UUID = UUID.random,
                                title: String
                              )

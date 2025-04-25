package com.epam.onadtochyi.ai.task.dto

import io.jvm.uuid._

import scala.collection.immutable

case class Conversation (
                        id:UUID = UUID.random,
                        title: String
                        //messages: immutable.Seq[Message] = immutable.Seq[Message]()
                        )


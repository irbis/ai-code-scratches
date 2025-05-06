package com.epam.onadtochyi.ai.task.conversation

object ConversationActionPerfomedStatus extends Enumeration {
  type ConversationActionPerfomedStatus = Value
  val DONE = Value("done")
  val ERR = Value("err")
}

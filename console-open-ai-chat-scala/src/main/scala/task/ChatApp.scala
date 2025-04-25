package com.epam.onadtochyi.ai
package task

import task.dto.{Conversation, Message, Model, Role}
import task.utils.Constant.{DEFAULT_SYSTEM_PROMPT, OPEN_AI_API_KEY}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

object ChatApp extends App {

  implicit val system: ActorSystem = ActorSystem("OpenAIClient")
  implicit val materializer: Materializer = Materializer(system)

  val openAIClient = OpenAIClient(Model.GPT_4o_MINI, OPEN_AI_API_KEY, false,
    system, materializer)

  println("Input SYSTEM prompt")
  print("> ")
  val sysPromptInput = scala.io.StdIn.readLine()
  val sysPrompt = if (sysPromptInput.isBlank) {
    println(s"Has been used default SYS prompt $DEFAULT_SYSTEM_PROMPT")
    DEFAULT_SYSTEM_PROMPT
  } else {
    println(s"Has been used SYS prompt $sysPromptInput")
    sysPromptInput
  }
  var conversation : Conversation = Conversation(Message(Role.SYSTEM, sysPrompt))

  println()
  println("Your question or exit to quit:")



  var continueWork = true;
  while (continueWork) {
    val input = scala.io.StdIn.readLine()
    if (input.toLowerCase().trim == "exit") {
      println("Exiting")
      continueWork = false
    } else {
      conversation = conversation.addMessage(Message(Role.USER, input))
//      openAIClient.postAndPrint(conversation.messages)
      openAIClient.getAndPrintStatus("https://google.com")
    }
    println()
  }
}

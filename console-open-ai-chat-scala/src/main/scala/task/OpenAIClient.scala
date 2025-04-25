package com.epam.onadtochyi.ai
package task

import task.dto.Message
import task.dto.Model.Model

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import java.net.http.HttpRequest
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class OpenAIClient private (
                     model: Model,
                     apiKey: String,
                     streamResponse: Boolean,
                     implicit val system: ActorSystem,
                     val materializer: Materializer)
{

  require(apiKey != null && !apiKey.trim.isEmpty)

  implicit val ec: ExecutionContext = ExecutionContext.global
  private val wsClient = StandaloneAhcWSClient()

  def getAndPrintStatus(URI: String): Unit =
    wsClient.url(URI)
      .withRequestTimeout(5.seconds)
      .withHttpHeaders("Accept" -> "text/html")
      .get()
      .map { response =>
        println(s"Response code from $URI is ${response.status}")
      }.onComplete(_ => wsClient.close())

  def postAndPrint(messages: List[Message]) : Message = ???

  def postRegularAndShowInConsole(httpRequest: HttpRequest, assistantResponse: String) : Unit = ???

  def postStreamAndShowInConsole(httpRequest: HttpRequest) : Unit = ???

  def collectAndPrintContent(data: String, assistantResponse: String) : Unit = ???
}

object OpenAIClient {
  def apply(model: Model, apiKey: String, streamResponse: Boolean,
            system: ActorSystem, materializer: Materializer): OpenAIClient =
    new  OpenAIClient(model, apiKey, streamResponse,
      system, materializer)
}

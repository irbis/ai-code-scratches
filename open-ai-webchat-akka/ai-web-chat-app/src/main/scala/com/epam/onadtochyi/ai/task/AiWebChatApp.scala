package com.epam.onadtochyi.ai.task

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route.seal
import com.epam.onadtochyi.ai.task.domain.model.Conversation
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.ExecutionContextExecutor

object AiWebChatApp extends AiJsonFormat {
  implicit val system: ActorSystem[Any] = ActorSystem(Behaviors.empty, "AiAkkaHttpServer")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  def main(args: Array[String]): Unit = {

    val route = {
      concat(
        pathPrefix("version") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "0.1"))
          }
        },
        pathPrefix("conversation" / LongNumber) { id =>
          get {
            val demoConversation = Conversation(title = s"Demo Conversation for tests with $id")
            complete(demoConversation)
          }
        }
      )
    }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(s"Server now online. Please connect to http://localhost:8080/hello\nPress RETURN to stop...")
    scala.io.StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}

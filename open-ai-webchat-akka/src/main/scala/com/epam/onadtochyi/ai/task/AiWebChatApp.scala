package com.epam.onadtochyi.ai.task

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.epam.onadtochyi.ai.task.registry.ConversationRegistry

import scala.util.{Failure, Success}

object AiWebChatApp {

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]) = {
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP server: {}", ex.getMessage)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    DatabaseSetup.init()

    val rootBehavior = Behaviors.setup[Nothing] { context => // create root actor in Akka applications, typical usage
      // this like new Class() but for actor - creates new object of actor
      val conversationRegistryActor = context.spawn(ConversationRegistry(DatabaseSetup.conversationDb), "ConversationRegistryActor")
      context.watch(conversationRegistryActor) // is watching for actors life cycle

      val routes = new ConversationRoutes(conversationRegistryActor)(context.system)
      startHttpServer(routes.conversationRoutes)(context.system)

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](rootBehavior, "AiAkkaHttpServer")
  }
}

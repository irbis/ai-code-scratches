package com.epam.onadtochyi.ai.task

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.epam.onadtochyi.ai.task.conversation.ConversationRegistry
import com.epam.onadtochyi.ai.task.conversationmessage.ConversationMessageRepositoryActor

import scala.util.{Failure, Success}

object AiWebChatApp {

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
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
      implicit val system: ActorSystem[Nothing] = context.system
      // this like new Class() but for actor - creates new object of actor
      val conversationRegistryActor = context.spawn(ConversationRegistry(DatabaseSetup.conversationDb), "ConversationRegistryActor")
      context.watch(conversationRegistryActor) // is watching for actors life cycle

      val conversationMessageRegistryActor = context.spawn(ConversationMessageRepositoryActor(DatabaseSetup.conversationMessageDb), "ConversationMessageRegistryActor")
      context.watch(conversationMessageRegistryActor)
      
      val conversationRoutes = new ConversationRoutes(conversationRegistryActor).conversationRoutes
      val conversationMessageRoutes = new ConversationMessageRoutes(conversationMessageRegistryActor).conversationMessageRoutes

      startHttpServer(conversationRoutes ~ conversationMessageRoutes)

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](rootBehavior, "AiAkkaHttpServer")
  }
}

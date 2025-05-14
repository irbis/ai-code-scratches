package com.epam.onadtochyi.ai.task

import com.epam.onadtochyi.ai.task.conversation.ConversationDatabase
import com.epam.onadtochyi.ai.task.conversationmessage.ConversationMessageDatabase
import slick.jdbc.HsqldbProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object DatabaseSetup {
  val db = Database.forConfig("database")

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  val conversationDb = new ConversationDatabase(db)
  val conversationMessageDb = new ConversationMessageDatabase(db)

  def init(): Future[Unit] = {
    conversationDb.createSchema()
      .map(_ => println("Conversation Database schema created"))
      .recover({ case ex => println(s"Error creating conversation database schema: ${ex.getMessage}") })

    conversationMessageDb.createSchema()
      .map(_ => println("Conversation Message Database schema created"))
      .recover({ case ex => println(s"Error creating conversation message database schema: ${ex.getMessage}") })
  }
}

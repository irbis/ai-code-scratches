package com.epam.onadtochyi.ai.task

import com.epam.onadtochyi.ai.task.registry.ConversationDatabase
import slick.jdbc.HsqldbProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object DatabaseSetup {
  val db = Database.forConfig("database")

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  val conversationDb = new ConversationDatabase(db)

  def init(): Future[Unit] = {
    conversationDb.createSchema()
      .map(_ => println("Database schema created"))
      .recover({ case ex => println(s"Error creating database schema: ${ex.getMessage}") })
  }
}

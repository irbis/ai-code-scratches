package com.epam.onadtochyi.ai.task.conversation

import io.jvm.uuid._
import slick.jdbc.HsqldbProfile.api._

import scala.concurrent.Future

case class ConversationRow(id: UUID, title: String)

class ConversationsTable(tag: Tag) extends Table[ConversationRow](tag, "CONVERSATIONS") {
  def id = column[UUID]("ID", O.PrimaryKey)
  def title = column[String]("TITLE")

  def * = (id, title) <> (ConversationRow.tupled, ConversationRow.unapply)
}

class ConversationDatabase(db: Database) {
  private val conversations = TableQuery[ConversationsTable]

  def createSchema(): Future[Unit] = {
    db.run(conversations.schema.create)
  }

  def insert(conversation: ConversationRow): Future[Int] = {
    db.run(conversations += conversation)
  }

  def getById(id: UUID): Future[Option[ConversationRow]] = {
    db.run(conversations.filter(_.id === id).result.headOption)
  }

  def getAllConversations: Future[Seq[ConversationRow]] = {
    db.run(conversations.result)
  }

  def deleteConversation(id: UUID): Future[Int] = {
    db.run(conversations.filter(_.id === id).delete)
  }
}
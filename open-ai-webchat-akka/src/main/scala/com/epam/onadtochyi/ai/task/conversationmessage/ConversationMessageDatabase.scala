package com.epam.onadtochyi.ai.task.conversationmessage

import com.epam.onadtochyi.ai.task.dto.Role
import com.epam.onadtochyi.ai.task.dto.Role.Role
import io.jvm.uuid._
import slick.ast.BaseTypedType
import slick.jdbc.HsqldbProfile.api._
import slick.jdbc.JdbcType

import scala.concurrent.{ExecutionContext, Future}

case class ConversationMessageRow(id: UUID, conversationId: UUID, content: String, role: Role)

class ConversationMessageTable(tag: Tag) extends Table[ConversationMessageRow](tag, "CONVERSATIONS_MESSAGE") {
  implicit val roleColumnType: BaseTypedType[Role] with JdbcType[Role] =
    MappedColumnType.base[Role, String](
      e => e.toString,
      s => Role.withName(s)
    )

  def id = column[UUID]("ID", O.PrimaryKey)
  def conversationId = column[UUID]("CONVERSATION_ID")

  def content = column[String]("CONTENT")

  def role = column[Role]("ROLE")

  def * = (id, conversationId, content, role) <> (ConversationMessageRow.tupled, ConversationMessageRow.unapply)
}

class ConversationMessageDatabase(db: Database)(implicit ec: ExecutionContext) {
  private val messages = TableQuery[ConversationMessageTable]

  def createSchema(): Future[Unit] = {
    db.run(messages.schema.create)
  }

  def getAllMessages: Future[Seq[ConversationMessageRow]] = {
    db.run(messages.result)
  }

  def getByConversationId(conversationId: UUID): Future[Seq[ConversationMessageRow]] = {
    db.run(messages.filter(_.conversationId === conversationId).result)
  }

  def getById(id: UUID): Future[Option[ConversationMessageRow]] = {
    db.run(messages.filter(_.id === id).result.headOption)
  }

  def insert(message: ConversationMessageRow): Future[Int] = {
    db.run(messages += message)
  }

  def deleteMessage(id: UUID): Future[Int] = {
    db.run(messages.filter(_.id === id).delete)
  }
}

package com.epam.onadtochyi.ai.task

import com.epam.onadtochyi.ai.task.AiJsonFormats.JsonFormats.enumFormat
import com.epam.onadtochyi.ai.task.dto.Conversation
import com.epam.onadtochyi.ai.task.conversation.ConversationRegistry.{AiActionPerformed, ConversationActionPerformed, GetConversationsResponse}
import com.epam.onadtochyi.ai.task.conversation.ConversationActionPerfomedStatus
import com.epam.onadtochyi.ai.task.conversationmessage.ConversationMessageRepositoryActor.{ConversationMessageActionPerformed, CreateConversationMessageRequest}
import com.epam.onadtochyi.ai.task.dto.Role
import io.jvm.uuid.UUID
import spray.json._

object AiJsonFormats {
  import DefaultJsonProtocol._

  implicit val uuidJsonFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    def write(uuid: UUID): JsString = spray.json.JsString(uuid.toString)
    def read(value: spray.json.JsValue): UUID = value match {
      case spray.json.JsString(uuid) => UUID.fromString(uuid)
      case _ => throw spray.json.DeserializationException("UUID expected")
    }
  }

  object JsonFormats extends DefaultJsonProtocol {
    implicit def enumFormat[T <: Enumeration](implicit enu: T): RootJsonFormat[T#Value] = {
      new RootJsonFormat[T#Value] {
        def write(obj: T#Value): JsValue = JsString(obj.toString)
        def read(json: JsValue): T#Value = json match {
          case JsString(str) => enu.withName(str)
          case _ => throw DeserializationException("Enum value expected")
        }
      }
    }


  }

  implicit val conversationJsonFormat: RootJsonFormat[Conversation] = jsonFormat2(Conversation.apply)
  implicit val getConversationJsonFormat: RootJsonFormat[GetConversationsResponse] = jsonFormat1(GetConversationsResponse.apply)

  implicit val aiActionPerformedJsonFormat: RootJsonFormat[AiActionPerformed] = jsonFormat1(AiActionPerformed.apply)
  implicit val conversationActionPerfomedStatus: RootJsonFormat[ConversationActionPerfomedStatus.Value] = enumFormat(ConversationActionPerfomedStatus)
  implicit val conversationActionPerformedJsonFormat: RootJsonFormat[ConversationActionPerformed] = jsonFormat3(ConversationActionPerformed.apply)

  implicit val messageRole: RootJsonFormat[Role.Value] = enumFormat(com.epam.onadtochyi.ai.task.dto.Role)
  implicit val createConversationMessageRequestFormat: RootJsonFormat[CreateConversationMessageRequest] = jsonFormat3(CreateConversationMessageRequest.apply)
  implicit val messageFormat: RootJsonFormat[com.epam.onadtochyi.ai.task.dto.Message] = jsonFormat3(com.epam.onadtochyi.ai.task.dto.Message.apply)
  implicit val conversationMessageActionPerformed: RootJsonFormat[ConversationMessageActionPerformed] = jsonFormat3(ConversationMessageActionPerformed.apply)
}

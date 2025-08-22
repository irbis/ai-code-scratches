package com.epam.onadtochyi.ai.task

import com.epam.onadtochyi.ai.task.domain.model.Conversation
import io.jvm.uuid._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, RootJsonFormat}

trait AiJsonFormat {
  implicit val uuidJsonFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    def write(uuid: UUID): JsString = spray.json.JsString(uuid.toString)
    def read(value: spray.json.JsValue): UUID = value match {
      case spray.json.JsString(uuid) => UUID.fromString(uuid)
      case _ => throw spray.json.DeserializationException("UUID expected")
    }
  }

  implicit val conversationFormat: RootJsonFormat[Conversation] = jsonFormat2(Conversation.apply)

}

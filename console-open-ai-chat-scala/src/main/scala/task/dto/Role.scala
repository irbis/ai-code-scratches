package com.epam.onadtochyi.ai
package task.dto

object Role extends Enumeration {
  type Role = Value
  val SYSTEM = Value("system")
  val USER = Value("user")
  val AI = Value("assistant")

  // TODO @JsonValue?
}

package com.epam.onadtochyi.ai
package task.dto

object Model extends Enumeration {
  type Model = Value

  val GPT_3_5_TURBO = Value("gpt-3.5-turbo")
  val GPT_4o_MINI = Value("gpt-4o-mini")
  val GPT_4o = Value("gpt-4o")
}

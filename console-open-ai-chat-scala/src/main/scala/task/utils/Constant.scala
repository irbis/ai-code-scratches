package com.epam.onadtochyi.ai
package task.utils

import java.net.URI

object Constant {
  final val OPEN_AI_API_URL: URI = URI.create("https://api.openai.com/v1/chat/completions")
  final val OPEN_AI_API_KEY: String = "put-your-api-key-here"

  final val DEFAULT_SYSTEM_PROMPT: String = "You are assistant who answers concisely and informatively."
}

package com.epam.onadtochyi.ai.task.dto

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RoleSpec extends AnyFlatSpec with Matchers {
  "Role" should "define SYSTEM role with correct string value" in {
    Role.SYSTEM.toString shouldBe "system"
  }

  it should "define USER role with correct string value" in {
    Role.USER.toString shouldBe "user"
  }

  it should "define AI role with correct string value" in {
    Role.AI.toString shouldBe "assistant"
  }

  it should "contain exactly 3 roles" in {
    Role.values.size shouldBe 3
  }
}

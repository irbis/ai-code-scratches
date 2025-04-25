package com.epam.onadtochyi.ai
package task.dto

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

  it should "have exactly three roles defined" in {
    Role.values.size shouldBe 3
  }

  it should "convert from string to Role value correctly" in {
    Role.withName("system") shouldBe Role.SYSTEM
    Role.withName("user") shouldBe Role.USER
    Role.withName("assistant") shouldBe Role.AI
  }

  it should "throw NoSuchElementException for invalid role name" in {
    assertThrows[NoSuchElementException] {
      Role.withName("invalid_role")
    }
  }
}
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "console-open-ai-chat-scala",
    idePackagePrefix := Some("com.epam.onadtochyi.ai"),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.2.11",
      //"com.typesafe.play" %% "play-ws-standalone-json" % "2.1.10",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    )
  )



lazy val akkaHttpVersion = "10.7.0"
lazy val akkaVersion    = "2.10.3"
lazy val slickVersion   = "3.5.0"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.epam.onadtochyi.ai",
      scalaVersion    := "2.13.16"
    )),
    name := "open-ai-webchat-akka",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-pki"                 % akkaVersion,
      "com.typesafe.slick" %% "slick"                   % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp"          % slickVersion,
      "io.jvm.uuid"       %% "scala-uuid"               % "0.3.1",
      "ch.qos.logback"    % "logback-classic"           % "1.5.17",
      "org.hsqldb"        % "hsqldb"                    % "2.7.2",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.12"        % Test
    )
  ).aggregate(aiWebChatApp)

// usefull web links:
// Akka http intro: https://doc.akka.io/libraries/akka-http/current/introduction.html
// About onion architecture: https://faun.pub/the-practical-onion-architecture-the-onion-that-doesnt-make-you-cry-f48e1b88355
// Example of implementation of onion architecture on java: https://github.com/splaw88/onion-architecture

lazy val aiWebChatApp = (project in file("ai-web-chat-app")).
  settings(
    inThisBuild(List(
      organization    := "com.epam.onadtochyi.ai",
      scalaVersion    := "2.13.16",
      resolvers += "Akka library repository".at("https://repo.akka.io/maven")
    )),
    name := "ai-web-chat-app",
    Compile / run / mainClass := Some("com.epam.onadtochyi.ai.task.AiWebChatApp"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-pki"                 % akkaVersion,
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "io.jvm.uuid"       %% "scala-uuid"               % "0.3.1"
    )
  ).dependsOn(domain)

val domain = (project in file("domain")).
  settings(
    inThisBuild(List(
      organization    := "com.epam.onadtochyi.ai",
      scalaVersion    := "2.13.16"
    )),
    name := "domain",
    libraryDependencies ++= Seq(
      "io.jvm.uuid"       %% "scala-uuid"               % "0.3.1"
    )
  )
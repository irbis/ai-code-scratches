lazy val akkaHttpVersion = "10.7.0"
lazy val akkaVersion    = "2.10.3"
lazy val slickVersion   = "3.5.0"

lazy val uuidDependency = Seq(
  "io.jvm.uuid" %% "scala-uuid" % "0.3.1"
)

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

inThisBuild(List(
  organization := "com.epam.onadtochyi.ai",
  scalaVersion := "2.13.16"
))

lazy val root = (project in file(".")).
  settings(
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

// application itself
lazy val aiWebChatApp = (project in file("ai-web-chat-app")).
  settings(
    name := "ai-web-chat-app",
    Compile / run / mainClass := Some("com.epam.onadtochyi.ai.task.AiWebChatApp"),
    libraryDependencies ++= uuidDependency ++ Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-pki"                 % akkaVersion,
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,

      "org.scalatest"     %% "scalatest"                % "3.2.12"        % Test
    )
  ).dependsOn(domain, repositoryInterface, logic)

// all project domain objects: conversation, etc.
val domain = (project in file("domain")).
  settings(
    name := "domain",
    libraryDependencies ++= uuidDependency
  )

// repository interfaces, no code related to particular data storage
val repositoryInterface = (project in file("repositoryinterface")).
  settings(
    name := "repositoryinterface",
    libraryDependencies ++= uuidDependency
  ).dependsOn(domain)

// application services interfaces without any implementation
val services = (project in file("services")).
  settings(
    name := "services"
  ).dependsOn(domain)

// logic contains implementation of services
val logic = (project in file("logic")).
  settings(
    name := "logic"
  ).dependsOn(services, repositoryInterface)

scalaVersion := "2.13.1"
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:higherKinds",
  "-unchecked",
  "-Xfatal-warnings",
)

val akkaVersion = "2.6.1"
val akkaHttpVersion = "10.1.11"
val circeVersion = "0.12.1"
val typesafeConfigVersion = "1.4.0"
val enumeratumVersion = "1.5.14"
val scalaTestVersion = "3.1.0"
val catsScalaTestVersion = "3.0.4"

val http4sVersion = "0.21.0-M6"

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
)

// Circe
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
)

// Configuration
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % typesafeConfigVersion,
)

// Enumeratum
libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % enumeratumVersion,
)

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.29",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
)

// Http4s
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
)

// Testing
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.ironcorelabs" %% "cats-scalatest" % catsScalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
)

// See https://github.com/augustjune/context-applied
addCompilerPlugin("org.augustjune" %% "context-applied" % "0.1.2")

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
val enumeratumVersion = "1.5.14"
val http4sVersion = "0.21.0-M6"
val slf4jVersion = "1.7.29"
val logbackVersion = "1.2.3"
val scalaLoggingVersion = "3.9.2"
val log4catsVersion = "1.0.1"
val pureConfigVersion = "0.12.2"
val scalaTestVersion = "3.1.0"
val catsScalaTestVersion = "3.0.4"

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

// Enumeratum
libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % enumeratumVersion,
)

// Http4s
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
)

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion,
)

// PureConfig
libraryDependencies ++= Seq(
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
)

// Testing
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.ironcorelabs" %% "cats-scalatest" % catsScalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
)

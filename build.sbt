scalaVersion := "2.12.8"

// Enable @JsonCodec macro annotation
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full,
)

val akkaVersion = "2.5.21"
val akkaHttpVersion = "10.1.7"
val circeVersion = "0.9.0"
val typesafeConfigVersion = "1.3.3"
val knutwalkerVersion = "3.5.0"
val scalaTestVersion = "3.0.5"

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
)

// Circe
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-java8" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
)

// Glues Akka and Circe
libraryDependencies ++= Seq(
  "de.knutwalker" %% "akka-http-json" % knutwalkerVersion,
  "de.knutwalker" %% "akka-stream-json" % knutwalkerVersion,
  "de.knutwalker" %% "akka-http-circe" % knutwalkerVersion,
  "de.knutwalker" %% "akka-stream-circe" % knutwalkerVersion,
)

// Configuration
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % typesafeConfigVersion,
)

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
)

// Testing
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
)

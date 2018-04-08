scalaVersion := "2.12.5"

// Enable @JsonCodec macro annotation
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

val akkaVersion = "2.5.11"
val akkaHttpVersion = "10.1.0"
val circeVersion = "0.9.1"
val knutwalkerVersion = "3.4.0"

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

// Akka HTTP
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
)

// Circe
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-java8" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

// Glues Akka and Circe
libraryDependencies ++= Seq(
  "de.knutwalker" %% "akka-http-json" % knutwalkerVersion,
  "de.knutwalker" %% "akka-stream-json" % knutwalkerVersion,
  "de.knutwalker" %% "akka-http-circe" % knutwalkerVersion,
  "de.knutwalker" %% "akka-stream-circe" % knutwalkerVersion
)

// Configuration
libraryDependencies += "com.typesafe" % "config" % "1.3.2"

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

// Testing
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

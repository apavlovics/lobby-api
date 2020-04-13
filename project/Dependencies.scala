import sbt._

object Dependencies {

  private val akkaVersion = "2.6.1"
  private val akkaHttpVersion = "10.1.11"
  private val catsScalaTestVersion = "3.0.4"
  private val circeVersion = "0.13.0"
  private val enumeratumVersion = "1.5.14"
  private val http4sVersion = "0.21.3"
  private val logbackVersion = "1.2.3"
  private val odinVersion = "0.6.0"
  private val pureConfigVersion = "0.12.3"
  private val scalaLoggingVersion = "3.9.2"
  private val scalaTestVersion = "3.1.0"

  object Akka {
    val ActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
    val ActorTestkitTyped = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion

    val Http = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
    val HttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion

    val StreamTyped = "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
    val StreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
  }

  val CatsScalaTest = "com.ironcorelabs" %% "cats-scalatest" % catsScalaTestVersion

  object Circe {
    val Core = "io.circe" %% "circe-core" % circeVersion
    val Generic = "io.circe" %% "circe-generic" % circeVersion
    val GenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion
    val Parser = "io.circe" %% "circe-parser" % circeVersion
  }

  val Enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion

  object Http4s {
    val BlazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
    val Dsl = "org.http4s" %% "http4s-dsl" % http4sVersion
  }

  val Logback = "ch.qos.logback" % "logback-classic" % logbackVersion

  object Odin {
    val Core = "com.github.valskalla" %% "odin-core" % odinVersion
    val Slf4j = "com.github.valskalla" %% "odin-slf4j" % odinVersion
  }

  object PureConfig {
    val Core = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
    val CatsEffect = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion
  }

  val ScalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
  val ScalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
}

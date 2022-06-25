import sbt._

object Dependencies {

  private val akkaVersion = "2.6.19"
  private val akkaHttpVersion = "10.2.9"
  private val catsEffectVersion = "3.2.9"
  private val enumeratumVersion = "1.7.0"
  private val http4sVersion = "1.0.0-M29"
  private val logbackVersion = "1.2.6"
  private val odinVersion = "0.13.0"
  private val pureConfigVersion = "0.17.0"
  private val scalaLoggingVersion = "3.9.4"
  private val scalaMockVersion = "5.2.0"
  private val scalaTestVersion = "3.2.12"
  private val scalaTestJsonVersion = "0.2.5"
  private val zioJsonVersion = "0.2.0-M4+15-312a4039-SNAPSHOT"

  object Akka {
    val ActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
    val ActorTestkitTyped = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion

    val Http = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
    val HttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion

    val StreamTyped = "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
    val StreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
  }

  object Cats {
    val EffectTestkit = "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion
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
  val ScalaMock = "org.scalamock" %% "scalamock" % scalaMockVersion

  object ScalaTest {
    val WordSpec = "org.scalatest" %% "scalatest-wordspec" % scalaTestVersion
    val ShouldMatchers = "org.scalatest" %% "scalatest-shouldmatchers" % scalaTestVersion
    val Json = "com.stephenn" %% "scalatest-json-jsonassert" % scalaTestJsonVersion
  }

  object ZIO {
    val Json = "dev.zio" %% "zio-json" % zioJsonVersion
  }
}

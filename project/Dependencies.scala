import sbt.*

object Dependencies {

  private val akkaVersion = "2.6.19"
  private val akkaHttpVersion = "10.2.9"
  private val catsVersion = "2.7.0"
  private val catsEffectVersion = "3.3.12"
  private val http4sVersion = "1.0.0-M33"
  private val jsonAssertVersion = "1.5.0"
  private val jsoniterScalaVersion = "2.13.32"
  private val logbackVersion = "1.2.11"
  private val odinVersion = "0.13.0"
  private val pureConfigVersion = "0.17.1"
  private val scalaLoggingVersion = "3.9.5"
  private val scalaTestVersion = "3.2.12"
  private val zioVersion = "2.0.15"
  private val zioLoggingVersion = "2.1.13"
  private val zioHttpVersion = "3.0.0-RC2"

  object Akka {

    val ActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
    val ActorTestkitTyped = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion

    val Http = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
    val HttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion

    val StreamTyped = "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
    val StreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
  }

  object Cats {
    val Core = "org.typelevel" %% "cats-core" % catsVersion
    val EffectTestkit = "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion
  }

  object Http4s {
    val BlazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
    val Dsl = "org.http4s" %% "http4s-dsl" % http4sVersion
  }

  val JsonAssert = "org.skyscreamer" % "jsonassert" % jsonAssertVersion

  object JsoniterScala {
    val Core = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterScalaVersion
    val Macros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterScalaVersion
  }

  val Logback = "ch.qos.logback" % "logback-classic" % logbackVersion

  object Odin {
    val Core = "com.github.valskalla" %% "odin-core" % odinVersion
    val Slf4j = "com.github.valskalla" %% "odin-slf4j" % odinVersion
  }

  object PureConfig {
    val Core = "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion
  }

  val ScalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion

  object ScalaTest {
    val WordSpec = "org.scalatest" %% "scalatest-wordspec" % scalaTestVersion
    val ShouldMatchers = "org.scalatest" %% "scalatest-shouldmatchers" % scalaTestVersion
  }

  object ZIO {
    val Core = "dev.zio" %% "zio" % zioVersion
    val Http = "dev.zio" %% "zio-http" % zioHttpVersion
    val LoggingSlf4j = "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion
    val TestSbt = "dev.zio" %% "zio-test-sbt" % zioVersion
  }
}

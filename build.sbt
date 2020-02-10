import Dependencies._

lazy val root = (project in file("."))
  .aggregate(
    config,
    protocol,
    akka,
    cats,
  )
  .settings(
    name := "evolution-gaming-lobby-api",
    inThisBuild(List(
      organization := "lv.continuum",
      scalaVersion := "2.13.1",
      scalacOptions ++= Seq(
        "-deprecation",
        "-feature",
        "-language:higherKinds",
        "-unchecked",
        "-Xfatal-warnings",
        "-Xlint:_",
      ),
    )),
  )

lazy val config = (project in file("config"))
  .settings(
    libraryDependencies ++= Seq(
      PureConfig.Core,
      PureConfig.CatsEffect,
    ),
  )

lazy val protocol = (project in file("protocol"))
  .settings(
    libraryDependencies ++= Seq(
      Circe.Core,
      Circe.Generic,
      Circe.GenericExtras,
      Circe.Parser,
      Enumeratum,

      CatsScalaTest % Test,
      ScalaTest % Test,
    ),
  )

lazy val akka = (project in file("akka"))
  .dependsOn(
    config,
    protocol % "compile->compile;test->test",
  )
  .settings(
    libraryDependencies ++= Seq(
      Akka.ActorTyped,
      Akka.Http,
      Akka.StreamTyped,
      Logback,
      ScalaLogging,
      Slf4j,

      Akka.ActorTestkitTyped % Test,
      Akka.HttpTestkit % Test,
      Akka.StreamTestkit % Test,
      ScalaTest % Test,
    ),
  )

lazy val cats = (project in file("cats"))
  .dependsOn(
    config,
    protocol,
  )
  .settings(
    libraryDependencies ++= Seq(
      Http4s.BlazeServer,
      Http4s.Dsl,
      Odin.Core,
      Odin.Slf4j,
    ),
  )

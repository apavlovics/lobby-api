import Dependencies._

lazy val root = (project in file("."))
  .aggregate(
    common,
    akka,
    cats,
  )
  .settings(
    name := "evolution-gaming-lobby-api",
    inThisBuild(
      List(
        organization := "lv.continuum",
        scalaVersion := "2.13.2",
        scalacOptions ++= Seq(
          "-deprecation",
          "-feature",
          "-language:higherKinds",
          "-unchecked",
          "-Xfatal-warnings",
          "-Xlint:_",
        ),
      )
    ),
  )

lazy val common = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      Circe.Core,
      Circe.Generic,
      Circe.GenericExtras,
      Circe.Parser,
      Enumeratum,
      PureConfig.Core,
      PureConfig.CatsEffect,
      CatsScalaTest % Test,
      ScalaTest % Test,
      ScalaMock % Test,
    ),
  )

lazy val akka = (project in file("akka"))
  .dependsOn(
    common % "compile->compile;test->test",
  )
  .settings(
    libraryDependencies ++= Seq(
      Akka.ActorTyped,
      Akka.Http,
      Akka.StreamTyped,
      Logback,
      ScalaLogging,
      Akka.ActorTestkitTyped % Test,
      Akka.HttpTestkit % Test,
      Akka.StreamTestkit % Test,
    ),
  )

lazy val cats = (project in file("cats"))
  .dependsOn(
    common % "compile->compile;test->test",
  )
  .settings(
    libraryDependencies ++= Seq(
      Http4s.BlazeServer,
      Http4s.Dsl,
      Odin.Core,
      Odin.Slf4j,
      Cats.Laws % Test,
    ),
  )

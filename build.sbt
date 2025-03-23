import Dependencies.*

// Check formatting and test
addCommandAlias("build", ";scalafmtCheckAll;test")

// Express that subproject depends on compile and test configuration of another subproject
val CompileTest = "compile->compile;test->test"

lazy val root = (project in file("."))
  .aggregate(
    common,
    akka,
    cats,
    zio,
  )
  .settings(
    name := "lobby-api",
    inThisBuild(
      List(
        organization := "lv.continuum",
        scalaVersion := "3.3.5",
        scalacOptions ++= Seq(
          "-deprecation",
          "-feature",
          "-new-syntax",
          "-no-indent",
          "-unchecked",
          "-Xfatal-warnings",
          "-Wunused:all",
          "-Wvalue-discard",
        ),
      )
    ),
  )

lazy val common = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      Cats.Core,
      Ip4s.Core,
      JsoniterScala.Core,
      JsoniterScala.Macros % Provided,
      PureConfig.Core,
      JsonAssert % Test,
      ScalaTest.WordSpec % Test,
      ScalaTest.ShouldMatchers % Test,
    ),
  )

lazy val akka = (project in file("akka"))
  .dependsOn(
    common % CompileTest,
  )
  .settings(
    libraryDependencies ++= Seq(
      Akka.ActorTyped,
      Akka.Http,
      Akka.StreamTyped,
      Akka.ActorTestkitTyped % Test,
      Akka.HttpTestkit % Test,
      Akka.StreamTestkit % Test,
    ).map(
      // TODO Remove once Akka HTTP supports Scala 3: https://github.com/akka/akka-http/issues/3891
      _ cross CrossVersion.for3Use2_13
    ),
    libraryDependencies ++= Seq(
      Logback,
      ScalaLogging,
    ),
  )

lazy val cats = (project in file("cats"))
  .dependsOn(
    common % CompileTest,
  )
  .settings(
    libraryDependencies ++= Seq(
      Http4s.Dsl,
      Http4s.EmberServer,
      Odin.Core,
      Odin.Slf4j,
      Cats.EffectTestkit % Test,
    ),
  )

lazy val zio = (project in file("zio"))
  .dependsOn(
    common % CompileTest,
  )
  .settings(
    libraryDependencies ++= Seq(
      ZIO.Core,
      ZIO.Http,
      ZIO.LoggingSlf4j,
      ZIO.TestSbt,
      Logback,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

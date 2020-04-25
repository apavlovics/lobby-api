package lv.continuum.evolution.cats

import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import org.scalatest.Assertions.fail

import scala.concurrent.duration._
import scala.concurrent.Future

trait IOSpec {

  implicit protected val context: TestContext = TestContext()
  implicit protected val cs: ContextShift[IO] = context.contextShift[IO]
  implicit protected val timer: Timer[IO] = context.timer[IO]
  implicit protected val logger: Logger[IO] = consoleLogger[IO](formatter = Formatter.colorful)

  def runTimed[A](io: IO[A])(implicit limit: Duration): A =
    io.unsafeRunTimed(limit).getOrElse(fail(s"Unable to complete test in $limit"))

  def runAsFuture[A](
    io: IO[A],
    tick: Boolean = true,
  ): Future[A] = {
    val future = io.unsafeToFuture()
    if (tick) context.tick()
    future
  }
}

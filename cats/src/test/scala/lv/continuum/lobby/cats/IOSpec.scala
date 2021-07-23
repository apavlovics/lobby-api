package lv.continuum.lobby.cats

import cats.effect.IO
import cats.effect.kernel.testkit.TestContext
import cats.effect.unsafe.implicits.global
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import org.scalatest.Assertions.fail

import scala.concurrent.Future
import scala.concurrent.duration._

trait IOSpec {

  implicit protected val context: TestContext = TestContext()
  implicit protected val logger: Logger[IO] = consoleLogger[IO](formatter = Formatter.colorful)

  def runTimed[A](io: IO[A])(implicit limit: FiniteDuration): A =
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

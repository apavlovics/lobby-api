package lv.continuum.lobby.cats

import cats.effect.IO
import cats.effect.kernel.testkit.TestContext
import cats.effect.unsafe.implicits.global
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import org.scalatest.Assertions.fail

import scala.concurrent.Future
import scala.concurrent.duration.*

trait IOSpec {

  protected given context: TestContext = TestContext()
  protected given logger: Logger[IO] = consoleLogger[IO](formatter = Formatter.colorful)

  def runTimed[A](io: IO[A])(using limit: FiniteDuration): A =
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

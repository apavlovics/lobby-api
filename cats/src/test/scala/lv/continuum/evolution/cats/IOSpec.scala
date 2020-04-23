package lv.continuum.evolution.cats

import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}
import io.odin.formatter.Formatter
import io.odin.{Logger, consoleLogger}
import org.scalatest.Assertions.fail

import scala.concurrent.duration._
import scala.concurrent.Future

trait IOSpec {

  protected implicit val context: TestContext = TestContext()
  protected implicit val cs: ContextShift[IO] = context.contextShift[IO]
  protected implicit val timer: Timer[IO] = context.timer[IO]
  protected implicit val logger: Logger[IO] = consoleLogger[IO](formatter = Formatter.colorful)

  def run[A](io: IO[A])(implicit limit: Duration): A =
    io.unsafeRunTimed(limit).getOrElse(fail(s"Unable to complete test in $limit"))

  def runAsFuture[A](io: IO[A]): Future[A] = io.unsafeToFuture()
}

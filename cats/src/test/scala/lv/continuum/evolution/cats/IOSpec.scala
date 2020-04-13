package lv.continuum.evolution.cats

import cats.effect.laws.util.TestContext
import cats.effect.{ContextShift, IO, Timer}
import org.scalatest.Assertions.fail

import scala.concurrent.duration._

trait IOSpec {

  protected implicit val context: TestContext = TestContext()
  protected implicit val cs: ContextShift[IO] = context.contextShift[IO]
  protected implicit val timer: Timer[IO] = context.timer[IO]

  def run[A](io: IO[A])(implicit limit: Duration): A =
    io.unsafeRunTimed(limit).getOrElse(fail(s"Unable to complete test in $limit"))
}

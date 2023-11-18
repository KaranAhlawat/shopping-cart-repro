package suite

import weaver.IOSuite
import weaver.scalacheck.Checkers
import weaver.scalacheck.CheckConfig
import cats.effect.Resource
import cats.effect.IO
import cats.syntax.all.*
import weaver.Expectations

abstract class ResourceSuite extends IOSuite with Checkers:
  override def checkConfig: CheckConfig =
    CheckConfig.default.copy(minimumSuccessful = 1)

  extension (res: Resource[IO, Res])
    def beforeAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.evalTap(f)

    def afterAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.flatTap(x => Resource.make(IO.unit)(_ => f(x)))

  def testBeforeAfterEach(
      before: Res => IO[Unit],
      after: Res => IO[Unit]
  ): String => (Res => IO[Expectations]) => Unit =
    name =>
      fa =>
        test(name): res =>
          before(res) >> fa(res).guarantee(after(res))

  def testAfterEach(
      after: Res => IO[Unit]
  ): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(_ => IO.unit, after)

  def testBeforeEach(
      before: Res => IO[Unit]
  ): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(before, _ => IO.unit)

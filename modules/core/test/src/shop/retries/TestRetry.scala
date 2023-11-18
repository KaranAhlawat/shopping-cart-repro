package shop.retries

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.syntax.option.*
import retry.*
import retry.RetryDetails.GivingUp
import scala.annotation.nowarn
import retry.RetryDetails.WillDelayAndRetry

object TestRetry:

  private[retries] def handlerFor[A <: RetryDetails](ref: Ref[IO, Option[A]]): Retry[IO] =
    new Retry[IO]:
      def retry[T](policy: RetryPolicy[IO], retriable: Retriable)(fa: IO[T]): IO[T] =
        @nowarn
        def onError(e: Throwable, details: RetryDetails): IO[Unit] =
          details match
            case a: A => ref.set(a.some)
            case _    => IO.unit

        retryingOnAllErrors[T](policy, onError)(fa)

  def givingUp(ref: Ref[IO, Option[GivingUp]]): Retry[IO] =
    handlerFor[GivingUp](ref)

  def recovering(ref: Ref[IO, Option[WillDelayAndRetry]]): Retry[IO] =
    handlerFor[WillDelayAndRetry](ref)

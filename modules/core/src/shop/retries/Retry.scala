package shop.retries

import cats.effect.Temporal
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import retry.RetryDetails.*
import retry.RetryPolicies.*
import retry.*

import scala.concurrent.duration.*

trait Retry[F[_]]:
  def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A]

object Retry:
  def apply[F[_]: Retry](using ev: Retry[F]): Retry[F] = ev

  def retryPolicy[F[_]: Temporal] = limitRetries(3) |+| exponentialBackoff(10.milliseconds)

  given [F[_]: Logger: Temporal]: Retry[F] with
    def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A] =
      def onError(e: Throwable, details: RetryDetails): F[Unit] =
        details match
          case WillDelayAndRetry(_, retriesSoFar, _) =>
            Logger[F].error(
              s"Failed to process ${retriable.toString} with ${e.getMessage}. So far we have retried $retriesSoFar times."
            )
          case GivingUp(totalRetries, _) =>
            Logger[F].error(s"Giving up on ${retriable.toString} after $totalRetries retries.")

      retryingOnAllErrors[A](policy, onError)(fa)

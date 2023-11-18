package shop.effect

import cats.effect.kernel.Sync

import java.time.Clock

trait JwtClock[F[_]]:
  def utc: F[Clock]

object JwtClock:
  def apply[F[_]: JwtClock](using ev: JwtClock[F]): JwtClock[F] = ev

  given [F[_]: Sync]: JwtClock[F] with
    def utc: F[Clock] = Sync[F].delay(Clock.systemUTC())

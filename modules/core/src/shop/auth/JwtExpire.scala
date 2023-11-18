package shop.auth

import shop.config.types.TokenExpiration
import shop.effect.JwtClock

import cats.effect.Sync
import cats.syntax.all.*
import pdi.jwt.JwtClaim

import java.time.Clock

trait JwtExpire[F[_]]:
  def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim]

object JwtExpire:
  def make[F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc.map:
      case given Clock =>
        new JwtExpire[F]:
          override def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim] =
            Sync[F].delay(
              claim
                .issuedNow
                .expiresIn(exp.value.toMillis)
            )

package shop.auth

import shop.config.types.{JwtAccessTokenKeyConfig, TokenExpiration}
import shop.effect.GenUUID

import cats.Monad
import cats.syntax.all.*
import dev.profunktor.auth.jwt.*
import io.circe.syntax.*
import pdi.jwt.{JwtAlgorithm, JwtClaim}

trait Tokens[F[_]]:
  def create: F[JwtToken]

object Tokens:
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      config: JwtAccessTokenKeyConfig,
      exp: TokenExpiration
  ): Tokens[F] =
    new Tokens[F]:
      override def create: F[JwtToken] =
        for
          uuid <- GenUUID[F].make
          claim <- jwtExpire.expiresIn(JwtClaim(uuid.asJson.noSpaces), exp)
          secretKey = JwtSecretKey(config.value)
          token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
        yield token

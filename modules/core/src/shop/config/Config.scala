package shop.config

import shop.config.types.*

import cats.effect.kernel.Async
import cats.syntax.all.*
import ciris.ConfigDecoder.*
import ciris.*
import com.comcast.ip4s.{host, port}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.ciris.given

import scala.concurrent.duration.*

object Config:
  def load[F[_]: Async]: F[AppConfig] =
    env("SC_APP_ENV")
      .as[AppEnvironment]
      .flatMap:
        case AppEnvironment.Test =>
          default[F](
            RedisURI("redis://localhost"),
            PaymentURI("https://payments.free.beecopter.com")
          )
        case AppEnvironment.Prod =>
          default[F](
            RedisURI("redis://localhost"),
            PaymentURI("https://payments.net/api")
          )
      .load[F]

  private def default[F[_]](
      redisUri: RedisURI,
      paymentUri: PaymentURI
  ): ConfigValue[F, AppConfig] =
    (
      env("SC_JWT_SECRET_KEY").as[JwtSecretKeyConfig].secret,
      env("SC_JWT_CLAIM").as[JwtClaimConfig].secret,
      env("SC_ACCESS_TOKEN_SECRET_KEY").as[JwtAccessTokenKeyConfig].secret,
      env("SC_ADMIN_USER_TOKEN").as[AdminUserTokenConfig].secret,
      env("SC_PASSWORD_SALT").as[PasswordSalt].secret,
      env("SC_POSTGRES_PASSWORD").as[NonEmptyString].secret
    ).parMapN((jwtSecretKey, jwtClaim, tokenKey, adminToken, salt, dbPassword) =>
      AppConfig(
        AdminJwtConfig(jwtSecretKey, jwtClaim, adminToken),
        tokenKey,
        salt,
        TokenExpiration(30.minutes.refine),
        ShoppingCartExpiration(30.minutes.refine),
        CheckoutConfig(
          retriesLimit = 3,
          retriesBackoff = 10.milliseconds
        ),
        PaymentConfig(paymentUri),
        HttpClientConfig(
          timeout = 60.seconds,
          idleTimeInPool = 30.seconds
        ),
        PostgreSQLConfig(
          host = "localhost",
          port = 5432,
          user = "postgres",
          password = dbPassword,
          database = "store",
          max = 10
        ),
        RedisConfig(redisUri),
        HttpServerConfig(
          host = host"0.0.0.0",
          port = port"8080"
        )
      )
    )

package shop.config

import ciris.Secret
import com.comcast.ip4s.{Host, Port}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

import scala.concurrent.duration.FiniteDuration

object types:
  type NonEmptyString = String :| ![Blank]
  type PosInt = Int :| Positive
  type UserPortNumber = Int :| (GreaterEqual[1024] & LessEqual[49151])

  opaque type AdminUserTokenConfig = String :| ![Blank]
  object AdminUserTokenConfig extends RefinedTypeOps[String, ![Blank], AdminUserTokenConfig]

  opaque type ShoppingCartExpiration = FiniteDuration :| Pure
  object ShoppingCartExpiration extends RefinedTypeOps[FiniteDuration, Pure, ShoppingCartExpiration]

  opaque type TokenExpiration = FiniteDuration :| Pure
  object TokenExpiration extends RefinedTypeOps[FiniteDuration, Pure, TokenExpiration]

  opaque type JwtSecretKeyConfig = String :| ![Blank]
  object JwtSecretKeyConfig extends RefinedTypeOps[String, ![Blank], JwtSecretKeyConfig]

  opaque type JwtAccessTokenKeyConfig = String :| ![Blank]
  object JwtAccessTokenKeyConfig extends RefinedTypeOps[String, ![Blank], JwtAccessTokenKeyConfig]

  opaque type JwtClaimConfig = String :| ![Blank]
  object JwtClaimConfig extends RefinedTypeOps[String, ![Blank], JwtClaimConfig]

  opaque type PasswordSalt = String :| ![Blank]
  object PasswordSalt extends RefinedTypeOps[String, ![Blank], PasswordSalt]

  opaque type RedisURI = String :| ![Blank]
  object RedisURI extends RefinedTypeOps[String, ![Blank], RedisURI]
  case class RedisConfig(uri: RedisURI)

  opaque type PaymentURI = String :| ![Blank]
  object PaymentURI extends RefinedTypeOps[String, ![Blank], PaymentURI]
  case class PaymentConfig(uri: PaymentURI)

  case class CheckoutConfig(
      retriesLimit: PosInt,
      retriesBackoff: FiniteDuration
  )

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

  case class AdminJwtConfig(
      secretKey: Secret[JwtSecretKeyConfig],
      claimStr: Secret[JwtClaimConfig],
      adminToken: Secret[AdminUserTokenConfig]
  )

  case class PostgreSQLConfig(
      host: NonEmptyString,
      port: PosInt, // create a UserPortNumber type
      user: NonEmptyString,
      password: Secret[NonEmptyString],
      database: NonEmptyString,
      max: PosInt
  )

  case class AppConfig(
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: Secret[JwtAccessTokenKeyConfig],
      passwordSalt: Secret[PasswordSalt],
      tokenExpiration: TokenExpiration,
      cartExpiration: ShoppingCartExpiration,
      checkoutConfig: CheckoutConfig,
      paymentConfig: PaymentConfig,
      httpClientConfig: HttpClientConfig,
      postgreSQL: PostgreSQLConfig,
      redis: RedisConfig,
      httpServerConfig: HttpServerConfig
  )

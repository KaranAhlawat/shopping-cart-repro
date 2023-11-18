package shop.domain

import cats.Eq
import cats.derived.*
import io.circe.Encoder
import io.github.iltotore.iron.*
import monocle.Iso

object healthcheck:
  enum Status derives Eq:
    case Okay
    case Unreachable

  object Status:
    val _Bool: Iso[Status, Boolean] =
      Iso[Status, Boolean](r =>
        r match
          case Okay        => true
          case Unreachable => false
      )(if _ then Okay else Unreachable)

  opaque type RedisStatus = Status :| Pure
  object RedisStatus extends RefinedTypeOps[Status, Pure, RedisStatus]
  opaque type PostgresStatus = Status :| Pure
  object PostgresStatus extends RefinedTypeOps[Status, Pure, PostgresStatus]

  case class AppStatus(redis: RedisStatus, postgres: PostgresStatus) derives Encoder.AsObject

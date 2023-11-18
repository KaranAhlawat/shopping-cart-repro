package shop.services

import shop.domain.healthcheck.*

import cats.effect.implicits.*
import cats.effect.{Resource, Temporal}
import cats.syntax.all.*
import dev.profunktor.redis4cats.RedisCommands
import io.github.iltotore.iron.refine
import skunk.*
import skunk.codec.numeric.int4
import skunk.implicits.*

import scala.concurrent.duration.*

trait HealthCheck[F[_]]:
  def status: F[AppStatus]

object HealthCheck:
  def make[F[_]: Temporal](
      postgres: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): HealthCheck[F] =
    new HealthCheck[F]:
      val q: Query[Void, Int] =
        sql"SELECT pid FROM pg_stat_activity".query(int4)

      val redisHealth: F[RedisStatus] =
        redis.ping
          .map(_.nonEmpty)
          .timeout(1.second)
          .map(Status._Bool.reverseGet)
          .orElse(Status.Unreachable.pure[F].widen)
          .map(status => RedisStatus(status.refine))

      val postgresHealth: F[PostgresStatus] =
        postgres
          .use(_.execute(q))
          .map(_.nonEmpty)
          .timeout(1.second)
          .map(Status._Bool.reverseGet)
          .orElse(Status.Unreachable.pure[F].widen)
          .map(status => PostgresStatus(status.refine))

      override def status: F[AppStatus] =
        (redisHealth, postgresHealth).parMapN(AppStatus.apply)

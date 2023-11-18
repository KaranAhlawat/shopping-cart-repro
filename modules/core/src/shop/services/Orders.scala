package shop.services

import shop.domain.ID
import shop.domain.auth.*
import shop.domain.cart.*
import shop.domain.item.ItemId
import shop.domain.order.*
import shop.effect.GenUUID
import shop.sql.codecs.*

import cats.data.NonEmptyList
import cats.effect.{Concurrent, Resource}
import cats.syntax.all.*
import io.github.iltotore.iron.circe.given
import skunk.*
import skunk.circe.codec.json.*
import skunk.implicits.*
import squants.market.Money

trait Orders[F[_]]:
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId]

object Orders:
  def make[F[_]: Concurrent: GenUUID](
      postgres: Resource[F, Session[F]]
  ): Orders[F] =
    new Orders[F]:
      import OrderSQL.*

      override def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
        postgres.use: session =>
          session.prepare(selectByUserIdAndOrderId).flatMap: q =>
            q.option((userId, orderId))

      override def findBy(userId: UserId): F[List[Order]] =
        postgres.use: session =>
          session.prepare(selectByUserId).flatMap: q =>
            q.stream(userId, 1024).compile.toList

      override def create(userId: UserId, paymentId: PaymentId, items: NonEmptyList[CartItem], total: Money): F[OrderId] =
        postgres.use: session =>
          session.prepare(insertOrder).flatMap: cmd =>
            ID.make[F, OrderId].flatMap: id =>
              val itemMap = items.toList.map(x => x.item.uuid -> x.quantity).toMap
              cmd.execute((userId, Order(id, paymentId, itemMap, total))).as(id)

private object OrderSQL:
  val decoder: Decoder[Order] =
    (
      orderId *: userId *: paymentId *:
        jsonb[Map[ItemId, Quantity]] *: money
    ).map((o, _, p, i, t) =>
      Order(o, p, i, t)
    )

  val encoder: Encoder[(UserId, Order)] =
    (
      orderId *: userId *: paymentId *:
        jsonb[Map[ItemId, Quantity]] *: money
    ).contramap((u, o) =>
      (o.uuid, u, o.pid, o.items, o.total)
    )

  val selectByUserId: Query[UserId, Order] =
    sql"""
    SELECT * FROM orders
    WHERE user_id = $userId
    """.query(decoder)

  val selectByUserIdAndOrderId: Query[(UserId, OrderId), Order] =
    sql"""
    SELECT * FROM orders
    WHERE user_id = $userId
    AND uuid = $orderId
    """.query(decoder)

  val insertOrder: Command[(UserId, Order)] =
    sql"""
    INSERT INTO orders
    VALUES ($encoder)
    """.command

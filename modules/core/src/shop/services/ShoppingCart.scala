package shop.services

import shop.config.types.ShoppingCartExpiration
import shop.domain.ID
import shop.domain.auth.*
import shop.domain.cart.*
import shop.domain.given
import shop.domain.item.*
import shop.effect.GenUUID

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.implicits.*
import dev.profunktor.redis4cats.RedisCommands
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given

trait ShoppingCart[F[_]]:
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]

object ShoppingCart:
  def make[F[_]: GenUUID: MonadCancelThrow](
      items: Items[F],
      redis: RedisCommands[F, String, String],
      exp: ShoppingCartExpiration
  ): ShoppingCart[F] =
    new ShoppingCart[F]:

      override def delete(userId: UserId): F[Unit] =
        redis.del(userId.show).void

      override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
        redis.hDel(userId.show, itemId.show).void

      override def get(userId: UserId): F[CartTotal] =
        redis.hGetAll(userId.show).flatMap: map =>
          map.toList
            .traverseFilter: (k, v) =>
              for
                id <- ID.read[F, ItemId](k)
                qt <- MonadThrow[F].catchNonFatal(Quantity(v.toInt.refine))
                rs <- items.findById(id).map(_.map(_.cart(qt)))
              yield rs
            .map: items =>
              CartTotal(items, items.foldMap(_.subTotal))

      override def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit] =
        redis.hSet(userId.show, itemId.show, quantity.show) *>
          redis.expire(userId.show, exp.value).void

      override def update(userId: UserId, cart: Cart): F[Unit] =
        redis.hGetAll(userId.show).flatMap:
          _.toList.traverse_((k, _) =>
            ID.read[F, ItemId](k).flatMap: id =>
              cart.value.get(id).traverse_(q =>
                redis.hSet(userId.show, k, q.show)
              )
          ) *> redis.expire(userId.show, exp.value).void

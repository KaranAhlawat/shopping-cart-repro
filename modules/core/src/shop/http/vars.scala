package shop.http

import shop.domain.item.ItemId
import shop.domain.order.OrderId

import cats.implicits.*
import io.github.iltotore.iron.refine

import java.util.UUID

object vars:
  protected class UUIDVar[A](f: UUID => A):
    def unapply(str: String): Option[A] = Either.catchNonFatal(f(UUID.fromString(str))).toOption

  object ItemdIdVar extends UUIDVar[ItemId](uuid => ItemId(uuid.refine))
  object OrderIdVar extends UUIDVar[OrderId](uuid => OrderId(uuid.refine))

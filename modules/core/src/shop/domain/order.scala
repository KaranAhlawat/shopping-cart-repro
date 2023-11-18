package shop.domain

import shop.domain.cart.*
import shop.domain.given
import shop.domain.item.*

import cats.Show
import cats.derived.*
import io.circe.Encoder
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.*
import squants.Money

import scala.util.control.NoStackTrace

import java.util.UUID

object order:
  opaque type OrderId = UUID :| Pure
  object OrderId extends RefinedTypeOps[UUID, Pure, OrderId]

  opaque type PaymentId = UUID :| Pure
  object PaymentId extends RefinedTypeOps[UUID, Pure, PaymentId]

  case class Order(uuid: OrderId, pid: PaymentId, items: Map[ItemId, Quantity], total: Money)
      derives Encoder.AsObject

  case object EmptyCartError extends NoStackTrace

  enum OrderOrPaymentError extends NoStackTrace derives Show:
    case OrderError(cause: String)
    case PaymentError(cause: String)

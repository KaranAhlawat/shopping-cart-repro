package shop.domain

import shop.domain.auth.UserId
import shop.domain.given
import shop.domain.item.*

import cats.Show
import cats.derived.*
import io.circe.*
import io.circe.generic.auto.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.*
import squants.market.{INR, Money}

import scala.util.control.NoStackTrace

object cart:
  opaque type Quantity = Int :| Pure
  object Quantity extends RefinedTypeOps[Int, Pure, Quantity]
  opaque type Cart = Map[ItemId, Quantity] :| Pure
  object Cart extends RefinedTypeOps[Map[ItemId, Quantity], Pure, Cart]

  case class CartItem(item: Item, quantity: Quantity) derives Show:
    def subTotal: Money =
      INR(item.price.amount * quantity)

  case class CartTotal(items: List[CartItem], total: Money) derives Encoder.AsObject, Show

  case class CartNotFound(userId: UserId) extends NoStackTrace

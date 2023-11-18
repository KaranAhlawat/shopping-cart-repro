package shop.programs

import shop.domain.auth.UserId
import shop.domain.cart.CartItem
import shop.domain.checkout.Card
import shop.domain.order.OrderOrPaymentError.*
import shop.domain.order.*
import shop.domain.payment.*
import shop.effect.Background
import shop.http.clients.PaymentClient
import shop.retries.*
import shop.services.{Orders, ShoppingCart}

import cats.*
import cats.data.*
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import retry.RetryPolicy
import squants.market.Money

import scala.concurrent.duration.*

final case class Checkout[F[_]: Logger: MonadThrow: Retry: Background](
    payment: PaymentClient[F],
    cart: ShoppingCart[F],
    orders: Orders[F],
    policy: RetryPolicy[F]
):
  private def ensureNonEmpty[A](xs: List[A]): F[NonEmptyList[A]] = xs
    .toNel
    .liftTo[F](EmptyCartError)

  def process(userId: UserId, card: Card): F[OrderId] =
    for
      c <- cart.get(userId)
      its <- ensureNonEmpty(c.items)
      pid <- processPayment(Payment(userId, c.total, card))
      oid <- createOrder(userId, pid, its, c.total)
      _ <- cart.delete(userId).attempt.void
    yield oid

  private def processPayment(in: Payment): F[PaymentId] = Retry[F]
    .retry(policy, Retriable.Payments)(payment.process(in))
    .adaptError:
      case e =>
        PaymentError(Option(e.getMessage).getOrElse("Unknown"))

  private def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId] =
    val action = Retry[F]
      .retry(policy, Retriable.Orders)(orders.create(userId, paymentId, items, total))
      .adaptError:
        case e =>
          OrderError(e.getMessage)

    def bgAction(fa: F[OrderId]): F[OrderId] = fa.onError:
      case _ =>
        Logger[F].error(s"Failed to create order: ${paymentId.toString}") *>
          Background[F].schedule(bgAction(fa), 1.hour)

    bgAction(action)

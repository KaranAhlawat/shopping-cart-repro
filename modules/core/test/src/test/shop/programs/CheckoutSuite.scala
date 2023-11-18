package test.shop.programs

import shop.domain.auth.*
import shop.domain.cart.*
import shop.domain.item.*
import shop.domain.order.OrderOrPaymentError.*
import shop.domain.order.*
import shop.domain.payment.Payment
import shop.effect.Background
import shop.effects.TestBackground
import shop.generators.*
import shop.http.clients.PaymentClient
import shop.programs.Checkout
import shop.retries.{Retry, TestRetry}
import shop.services.*

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.syntax.all.*
import io.github.iltotore.iron.cats.given
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry.RetryPolicies.limitRetries
import retry.{RetryDetails, RetryPolicy}
import squants.market.{INR, Money}
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.concurrent.duration.*
import scala.util.control.NoStackTrace

object CheckoutSuite extends SimpleIOSuite with Checkers:
  val MaxRetries = 3
  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(pid: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO]:
      override def process(payment: Payment): IO[PaymentId] = IO.pure(pid)

  val unreachableClient: PaymentClient[IO] =
    new PaymentClient[IO]:
      override def process(payment: Payment): IO[PaymentId] =
        IO.raiseError(PaymentError(""))

  def recoveringClient(
      attemptsSoFar: Ref[IO, Int],
      paymentId: PaymentId
  ): PaymentClient[IO] =
    new PaymentClient[IO]:
      override def process(payment: Payment): IO[PaymentId] =
        attemptsSoFar.get.flatMap:
          case n if n === 1 =>
            paymentId.pure
          case _: Int =>
            attemptsSoFar.update(_ + 1) *>
              PaymentError("").raiseError

  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] =
    new TestCart:
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)
      override def delete(userId: UserId): IO[Unit] = IO.unit

  val emptyCart: ShoppingCart[IO] =
    new TestCart:
      override def get(userId: UserId): IO[CartTotal] = IO.pure(CartTotal(List.empty, INR(0)))

  def failingCart(cartTotal: CartTotal): ShoppingCart[IO] =
    new TestCart:
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)
      override def delete(userId: UserId): IO[Unit] = (new NoStackTrace {}).raiseError

  def successfulOrders(oid: OrderId): Orders[IO] =
    new TestOrders:
      override def create(
          userId: UserId,
          paymentId: PaymentId,
          items: NonEmptyList[CartItem],
          total: Money
      ): IO[OrderId] =
        IO.pure(oid)

  val failingOrders: Orders[IO] =
    new TestOrders:
      override def create(
          userId: UserId,
          paymentId: PaymentId,
          items: NonEmptyList[CartItem],
          total: Money
      ): IO[OrderId] =
        OrderError("").raiseError

  val gen =
    for
      uid <- userIdGen
      pid <- paymentIdGen
      oid <- orderIdGen
      crt <- cartTotalGen
      crd <- cardGen
    yield (uid, pid, oid, crt, crd)

  given Background[IO] = TestBackground.NoOp
  given Logger[IO] = NoOpLogger[IO]

  test("successful checkout"):
    forall(gen): (uid, pid, oid, crt, crd) =>
      Checkout[IO](
        successfulClient(pid),
        successfulCart(crt),
        successfulOrders(oid),
        retryPolicy
      ).process(
        userId = uid,
        card = crd
      ).map(expect.same(oid, _))

  test("empty cart"):
    forall(gen): (uid, pid, oid, _, crd) =>
      Checkout[IO](
        successfulClient(pid),
        emptyCart,
        successfulOrders(oid),
        retryPolicy
      ).process(uid, crd)
        .attempt
        .map:
          case Left(EmptyCartError) => success
          case _                    => failure("Cart was not empty as expected")

  test("unreachable payment client"):
    forall(gen): (uid, _, oid, crt, crd) =>
      Ref.of[IO, Option[GivingUp]](None).flatMap: retries =>
        given Retry[IO] = TestRetry.givingUp(retries)

        Checkout[IO](
          unreachableClient,
          successfulCart(crt),
          successfulOrders(oid),
          retryPolicy
        ).process(uid, crd)
          .attempt
          .flatMap:
            case Left(PaymentError(_)) =>
              retries.get.map:
                case Some(g) => expect.same(g.totalRetries, MaxRetries)
                case None    => failure("expected GivingUp")
            case _ => failure("Expected payment error").pure

  test("failing client succeeds after one retry"):
    forall(gen): (uid, pid, oid, crt, crd) =>
      (
        Ref.of[IO, Option[WillDelayAndRetry]](None),
        Ref.of[IO, Int](0)
      ).tupled.flatMap: (retries, cliRef) =>
        given Retry[IO] = TestRetry.recovering(retries)

        Checkout[IO](
          recoveringClient(cliRef, pid),
          successfulCart(crt),
          successfulOrders(oid),
          retryPolicy
        ).process(uid, crd)
          .attempt
          .flatMap:
            case Right(id) =>
              retries.get.map:
                case Some(w) =>
                  expect.same(id, oid) |+| expect.same(0, w.retriesSoFar)
                case None =>
                  failure("Expected one retry")
            case Left(_) => failure("Expected Payment Id").pure

  test("cannot create order, run in background"):
    forall(gen): (uid, pid, _, crt, crd) =>
      (
        Ref.of[IO, (Int, FiniteDuration)](0, 0.seconds),
        Ref.of[IO, Option[GivingUp]](None)
      ).tupled.flatMap: (acc, retries) =>
        given Background[IO] = TestBackground.counter(acc)
        given Retry[IO] = TestRetry.givingUp(retries)

        Checkout[IO](
          successfulClient(pid),
          successfulCart(crt),
          failingOrders,
          retryPolicy
        ).process(uid, crd)
          .attempt
          .flatMap:
            case Left(OrderError(_)) =>
              (acc.get, retries.get).mapN:
                case (c, Some(g)) =>
                  expect.same(c, 1 -> 1.hour) |+| expect.same(g.totalRetries, MaxRetries)
                case _ => failure(s"Expected $MaxRetries retries and reschedule")
            case _ => failure("Expected order error").pure

  test("failing to delete cart does not affect checkout"):
    forall(gen): (uid, pid, oid, crt, crd) =>
      Checkout[IO](
        successfulClient(pid),
        failingCart(crt),
        successfulOrders(oid),
        retryPolicy
      ).process(uid, crd)
        .map(expect.same(oid, _))

protected class TestOrders() extends Orders[IO]:
  def get(userId: UserId, orderId: OrderId): IO[Option[Order]] = ???
  def findBy(userId: UserId): IO[List[Order]] = ???
  def create(userId: UserId, paymentId: PaymentId, items: NonEmptyList[CartItem], total: Money): IO[OrderId] = ???

protected class TestCart() extends ShoppingCart[IO]:
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???
  def get(userId: UserId): IO[CartTotal] = ???
  def delete(userId: UserId): IO[Unit] = ???
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit] = ???
  def update(userId: UserId, cart: Cart): IO[Unit] = ???

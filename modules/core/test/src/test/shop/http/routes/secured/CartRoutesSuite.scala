package test.shop.http.routes.secured

import shop.domain.auth.UserId
import shop.domain.cart.{Cart, CartTotal, Quantity}
import shop.domain.item.ItemId
import shop.generators.{cartGen, cartTotalGen, commonUserGen}
import shop.http.auth.auth.CommonUser
import shop.http.routes.secured.CartRoutes
import shop.services.ShoppingCart

import cats.data.Kleisli
import cats.effect.IO
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.circe.given
import org.http4s.Method.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.dsl.io.*
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals.*
import squants.market.INR
import suite.HttpSuite

object CartRoutesSuite extends HttpSuite:
  def authMiddleware(
      authUser: CommonUser
  ): AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  def dataCart(cartTotal: CartTotal): ShoppingCart[IO] =
    new TestShoppingCart:
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)

  test("GET shopping cart succeeds"):
    val gen =
      for
        u <- commonUserGen
        c <- cartTotalGen
      yield (u, c)

    forall(gen): (u, c) =>
      val req = GET(uri"/cart")
      val routes =
        CartRoutes[IO](dataCart(c))
          .routes(authMiddleware(u))
      expectHttpBodyAndStatus(routes, req)(c, Status.Ok)

  test("POST add item to shopping cart succeeds"):
    val gen =
      for
        u <- commonUserGen
        c <- cartGen
      yield (u, c)

    forall(gen): (user, cart) =>
      val req = POST(cart, uri"/cart")
      val routes =
        CartRoutes[IO](TestShoppingCart())
          .routes(authMiddleware(user))
      expectHttpStatus(routes, req)(Status.Created)

protected class TestShoppingCart extends ShoppingCart[IO]:
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = IO.unit
  def get(userId: UserId): IO[CartTotal] =
    IO.pure(CartTotal(List.empty, INR(0)))
  def delete(userId: UserId): IO[Unit] = IO.unit
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit] = IO.unit
  def update(userId: UserId, cart: Cart): IO[Unit] = IO.unit

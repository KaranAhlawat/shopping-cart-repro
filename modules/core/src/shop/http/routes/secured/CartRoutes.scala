package shop.http.routes.secured

import shop.domain.cart.Cart
import shop.http.auth.auth.CommonUser
import shop.http.vars.ItemdIdVar
import shop.services.ShoppingCart

import cats.Monad
import cats.syntax.all.*
import io.github.iltotore.iron.circe.given
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class CartRoutes[F[_]: JsonDecoder: Monad](shoppingCart: ShoppingCart[F])
    extends Http4sDsl[F]:
  private[routes] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of:
    case GET -> Root as user =>
      Ok(shoppingCart.get(user.id))

    case ar @ POST -> Root as user =>
      ar.req
        .asJsonDecode[Cart]
        .flatMap: cart =>
          cart
            .value
            .map: (id, quantity) =>
              shoppingCart.add(user.id, id, quantity)
            .toList
            .sequence *> Created()

    case ar @ PUT -> Root as user =>
      ar.req
        .asJsonDecode[Cart]
        .flatMap: cart =>
          shoppingCart.update(user.id, cart) *> Ok()

    case DELETE -> Root / ItemdIdVar(itemId) as user =>
      shoppingCart.removeItem(user.id, itemId) *> NoContent()

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

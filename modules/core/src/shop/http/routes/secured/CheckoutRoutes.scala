package shop.http.routes.secured

import shop.domain.cart.CartNotFound
import shop.domain.checkout.Card
import shop.domain.order.*
import shop.ext.http4s.iron.*
import shop.http.auth.auth.CommonUser
import shop.programs.Checkout

import cats.MonadThrow
import cats.syntax.all.*
import io.github.iltotore.iron.circe.given
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class CheckoutRoutes[F[_]: JsonDecoder: MonadThrow](checkout: Checkout[F])
    extends Http4sDsl[F]:
  private[routes] val prefixPath = "/checkout"
  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of:
    case ar @ POST -> Root as user =>
      ar.req
        .decodeR[Card]: card =>
          checkout
            .process(user.id, card)
            .flatMap(Created(_))
            .recoverWith:
              case CartNotFound(userId) =>
                NotFound(s"Cart not found for user: ${userId}")
              case EmptyCartError =>
                BadRequest("Shopping cart is empty")
              case e: OrderOrPaymentError =>
                BadRequest(e.show)

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

package shop.http.routes.secured

import shop.http.auth.auth.CommonUser
import shop.http.vars.OrderIdVar
import shop.services.Orders

import cats.Monad
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class OrderRoutes[F[_]: Monad](orders: Orders[F]) extends Http4sDsl[F]:
  private[routes] val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of:
    case GET -> Root as user =>
      Ok(orders.findBy(user.id))

    case GET -> Root / OrderIdVar(orderId) as user =>
      Ok(orders.get(user.id, orderId))

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

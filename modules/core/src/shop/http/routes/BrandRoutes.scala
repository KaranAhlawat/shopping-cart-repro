package shop.http.routes

import shop.domain.brand.*
import shop.services.Brands

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class BrandRoutes[F[_]: Monad](brands: Brands[F]) extends Http4sDsl[F]:
  private[routes] val prefixPath = "/brands"

  private val httpRoutes = HttpRoutes.of[F]:
    case GET -> Root =>
      Ok(brands.findAll)

  val routes = Router(prefixPath -> httpRoutes)

package shop.http.routes

import shop.services.Categories

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class CategoryRoutes[F[_]: Monad](categories: Categories[F]) extends Http4sDsl[F]:
  private[routes] val prefixPath = "/categories"

  private val httpRoutes = HttpRoutes.of[F]:
    case GET -> Root =>
      Ok(categories.findAll)

  val routes = Router(prefixPath -> httpRoutes)

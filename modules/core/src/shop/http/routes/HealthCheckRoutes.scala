package shop.http.routes

import shop.services.HealthCheck

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class HealthCheckRoutes[F[_]: Monad](healthCheck: HealthCheck[F]) extends Http4sDsl[F]:
  private[routes] val prefixPath = "/healthcheck"

  private val httpRoutes = HttpRoutes.of[F]:
    case GET -> Root =>
      Ok(healthCheck.status)

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)

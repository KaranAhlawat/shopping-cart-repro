package shop.http.routes.admin

import shop.domain.brand.BrandParam
import shop.ext.http4s.iron.decodeR
import shop.http.auth.auth.AdminUser
import shop.services.Brands

import cats.MonadThrow
import cats.syntax.flatMap.*
import io.circe.JsonObject
import io.circe.syntax.*
import io.github.iltotore.iron.circe.given
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final case class AdminBrandRoutes[F[_]: JsonDecoder: MonadThrow](brands: Brands[F])
    extends Http4sDsl[F]:
  private[admin] val prefixPath = "/brands"

  private val httpRoutes = AuthedRoutes.of[AdminUser, F]:
    case ar @ POST -> Root as _ =>
      ar.req
        .decodeR[BrandParam]: bp =>
          brands
            .create(bp.toDomain)
            .flatMap: id =>
              Created(JsonObject.singleton("brand_id", id.asJson))

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

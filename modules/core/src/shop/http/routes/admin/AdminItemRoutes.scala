package shop.http.routes.admin

import shop.domain.item.*
import shop.ext.http4s.iron.decodeR
import shop.http.auth.auth.AdminUser
import shop.services.Items

import cats.MonadThrow
import cats.syntax.flatMap.*
import io.circe.JsonObject
import io.circe.syntax.*
import io.github.iltotore.iron.circe.given
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final case class AdminItemRoutes[F[_]: JsonDecoder: MonadThrow](items: Items[F])
    extends Http4sDsl[F]:
  private[admin] val prefixPath = "/items"

  private val httpRoutes = AuthedRoutes.of[AdminUser, F]:
    case ar @ POST -> Root as _ =>
      ar.req
        .decodeR[CreateItemParam]: cip =>
          items
            .create(cip.toDomain)
            .flatMap: id =>
              Created(JsonObject.singleton("item_id", id.asJson))

    case ar @ PUT -> Root as _ =>
      ar.req
        .decodeR[UpdateItemParam]: uip =>
          items.update(uip.toDomain) >> Ok()

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]) = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

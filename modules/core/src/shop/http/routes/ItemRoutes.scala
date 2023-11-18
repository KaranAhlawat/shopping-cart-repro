package shop.http.routes

import shop.domain.brand.*
import shop.ext.http4s.iron.given
import shop.services.Items

import cats.Monad
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class ItemRoutes[F[_]: Monad](items: Items[F]) extends Http4sDsl[F]:
  private[routes] val prefixPath = "/items"

  object BrandParamQuery extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private val httpRoutes = HttpRoutes.of[F]:
    case GET -> Root :? BrandParamQuery(brand) =>
      Ok(brand.fold(items.findAll)(b => items.findBy(b.toDomain)))

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)

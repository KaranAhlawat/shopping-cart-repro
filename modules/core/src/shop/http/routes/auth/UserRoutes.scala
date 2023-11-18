package shop.http.routes.auth

import shop.domain.auth.*
import shop.ext.http4s.iron.*
import shop.services.Auth

import cats.MonadThrow
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class UserRoutes[F[_]: JsonDecoder: MonadThrow](auth: Auth[F]) extends Http4sDsl[F]:
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "users" =>
      req.decodeR[CreateUser]: user =>
        auth
          .newUser(user.username.toDomain, user.password.toDomain)
          .flatMap(tkn => Created(tkn.value))
          .recoverWith:
            case UserNameInUse(username) =>
              Conflict(username.toString)

  val routes = Router(prefixPath -> httpRoutes)

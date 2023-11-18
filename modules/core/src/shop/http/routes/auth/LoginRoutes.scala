package shop.http.routes.auth

import shop.domain.auth.*
import shop.ext.http4s.iron.*
import shop.services.Auth

import cats.MonadThrow
import cats.syntax.all.*
import io.circe.Encoder
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.given
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final case class LoginRoutes[F[_]: MonadThrow: JsonDecoder](auth: Auth[F]) extends Http4sDsl[F]:
  private[routes] val prefixPath = "/auth"
  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "login" =>
      req.decodeR[LoginUser]: user =>
        auth
          .login(user.username.toDomain, user.password.toDomain)
          .flatMap(jwtToken => Ok(jwtToken.value))
          .recoverWith:
            case UserNotFound(_) | InvalidPassword(_) =>
              Forbidden()

  val routes = Router(prefixPath -> httpRoutes)

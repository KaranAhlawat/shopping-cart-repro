package shop.services

import shop.auth.{Crypto, Tokens}
import shop.config.types.*
import shop.domain.auth.*
import shop.domain.given
import shop.http.auth.auth.*

import cats.*
import cats.syntax.all.*
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import io.circe.syntax.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.refine
import pdi.jwt.JwtClaim

trait Auth[F[_]]:
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]

object Auth:
  def make[F[_]: MonadThrow](
      tokenExpiration: TokenExpiration,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String],
      crypto: Crypto
  ): Auth[F] = new Auth[F]:
    private val TokenExpiration = tokenExpiration.value

    override def newUser(username: UserName, password: Password): F[JwtToken] =
      users.find(username).flatMap(uwp =>
        uwp match
          case Some(_) => UserNameInUse(username).raiseError[F, JwtToken]
          case None =>
            for
              id <- users.create(username, crypto.encrypt(password))
              t <- tokens.create
              u = User(id, username).asJson.noSpaces
              _ <- redis.setEx(t.value, u, TokenExpiration)
              _ <- redis.setEx(username.show, t.value, TokenExpiration)
            yield t
      )

    override def login(username: UserName, password: Password): F[JwtToken] =
      users.find(username).flatMap(uwp =>
        uwp match
          case None => UserNotFound(username).raiseError[F, JwtToken]
          case Some(user) if user.password =!= crypto.encrypt(password) =>
            InvalidPassword(username).raiseError[F, JwtToken]
          case Some(user) =>
            redis.get(username.show).flatMap(token =>
              token match
                case None =>
                  tokens.create.flatTap(t =>
                    redis.setEx(
                      t.value,
                      user.asJson.noSpaces,
                      TokenExpiration
                    ) *> redis.setEx(username.show, t.value, TokenExpiration)
                  )
                case Some(value) => JwtToken(value).pure[F]
            )
      )

    override def logout(token: JwtToken, username: UserName): F[Unit] =
      redis.del(token.show) *> redis.del(username.show).void

trait UsersAuth[F[_], A]:
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]

object UsersAuth:
  def common[F[_]: Functor](
      redis: RedisCommands[F, String, String]
  ): UsersAuth[F, CommonUser] =
    new UsersAuth[F, CommonUser]:
      override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
        redis
          .get(token.value)
          .map:
            _.flatMap: u =>
              decode[User](u).toOption.map(u => CommonUser.apply(u.refine))

  def admin[F[_]: Applicative](
      adminToken: JwtToken,
      adminUser: AdminUser
  ): UsersAuth[F, AdminUser] =
    new UsersAuth[F, AdminUser]:
      override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
        (token === adminToken)
          .guard[Option]
          .as(adminUser)
          .pure[F]

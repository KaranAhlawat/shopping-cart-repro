package shop.services

import shop.domain.ID
import shop.domain.auth.*
import shop.effect.GenUUID
import shop.http.auth.auth.*
import shop.sql.codecs.*

import cats.effect.{MonadCancelThrow, Resource}
import cats.syntax.all.*
import skunk.*
import skunk.implicits.*

trait Users[F[_]]:
  def find(username: UserName): F[Option[UserWithPassword]]
  def create(username: UserName, encryptedPassword: EncryptedPassword): F[UserId]

object Users:
  def make[F[_]: GenUUID: MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): Users[F] =
    new Users[F]:
      import UserSQL.*

      override def find(username: UserName): F[Option[UserWithPassword]] =
        postgres.use: session =>
          session.prepare(selectUser).flatMap: q =>
            q.option(username).map(r =>
              r match
                case Some((u, p)) => UserWithPassword(u.id, u.name, p).some
                case None         => none[UserWithPassword]
            )

      override def create(username: UserName, encryptedPassword: EncryptedPassword): F[UserId] =
        postgres.use: session =>
          session.prepare(insertUser).flatMap: cmd =>
            ID.make[F, UserId].flatMap: id =>
              cmd
                .execute((User(id, username), encryptedPassword))
                .as(id)
                .recoverWith:
                  case SqlState.UniqueViolation(_) =>
                    UserNameInUse(username).raiseError[F, UserId]

private object UserSQL:
  val codec: Codec[(User, EncryptedPassword)] =
    (userId *: userName *: encPassword).imap((i, n, p) => (User(i, n), p))((u, p) => (u.id, u.name, p))

  val selectUser: Query[UserName, (User, EncryptedPassword)] =
    sql"""
    SELECT * FROM users
    WHERE name = $userName
    """.query(codec)

  val insertUser: Command[(User, EncryptedPassword)] =
    sql"""
    INSERT INTO users
    VALUES ($codec)
    """.command

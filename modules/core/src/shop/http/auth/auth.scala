package shop.http.auth

import shop.domain.auth.*

import cats.Show
import cats.derived.*
import dev.profunktor.auth.jwt.*
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.circe.given

object auth:
  opaque type AdminJwtAuth = JwtSymmetricAuth :| Pure
  object AdminJwtAuth extends RefinedTypeOps[JwtSymmetricAuth, Pure, AdminJwtAuth]
  opaque type UserJwtAuth = JwtSymmetricAuth :| Pure
  object UserJwtAuth extends RefinedTypeOps[JwtSymmetricAuth, Pure, UserJwtAuth]

  case class User(id: UserId, name: UserName) derives Decoder, Encoder.AsObject, Show

  case class UserWithPassword(id: UserId, name: UserName, password: EncryptedPassword) derives Encoder.AsObject

  opaque type CommonUser <: User :| Pure = User :| Pure
  object CommonUser extends RefinedTypeOps[User, Pure, CommonUser]
  opaque type AdminUser <: User :| Pure = User :| Pure
  object AdminUser extends RefinedTypeOps[User, Pure, AdminUser]

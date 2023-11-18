package shop.domain
import io.circe.{Codec, Decoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.all.*

import scala.annotation.targetName
import scala.util.control.NoStackTrace

import java.util.UUID
import javax.crypto.Cipher

object auth:
  opaque type UserId = UUID :| Pure
  object UserId extends RefinedTypeOps[UUID, Pure, UserId]
  opaque type UserName = String :| Pure
  object UserName extends RefinedTypeOps[String, Pure, UserName]
  opaque type Password = String :| Pure
  object Password extends RefinedTypeOps[String, Pure, Password]
  opaque type EncryptedPassword = String :| Pure
  object EncryptedPassword extends RefinedTypeOps[String, Pure, EncryptedPassword]

  opaque type UserNameParam = String :| Not[Blank]
  object UserNameParam extends RefinedTypeOps[String, Not[Blank], UserNameParam]
  extension (x: UserNameParam)
    def toDomain: UserName = UserName(x.toLowerCase.refine)

  opaque type PasswordParam = String :| Not[Blank]
  object PasswordParam extends RefinedTypeOps[String, Not[Blank], PasswordParam]
  extension (x: PasswordParam)
    @targetName("toDomainPasswordParam")
    def toDomain: Password = Password(x.refine)

  case class CreateUser(username: UserNameParam, password: PasswordParam) derives Codec.AsObject

  case class UserNotFound(username: UserName) extends NoStackTrace
  case class UserNameInUse(username: UserName) extends NoStackTrace
  case class InvalidPassword(username: UserName) extends NoStackTrace

  case class LoginUser(username: UserNameParam, password: PasswordParam) derives Codec.AsObject

  opaque type EncryptCipher = Cipher :| Pure
  object EncryptCipher extends RefinedTypeOps[Cipher, Pure, EncryptCipher]

  opaque type DecryptCipher = Cipher :| Pure
  object DecryptCipher extends RefinedTypeOps[Cipher, Pure, DecryptCipher]

  opaque type ClaimContent = UUID :| Pure
  object ClaimContent extends RefinedTypeOps[UUID, Pure, ClaimContent]:
    given Decoder[ClaimContent] =
      Decoder.forProduct1("uuid")(ClaimContent.applyUnsafe(_))

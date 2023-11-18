package shop.domain

import shop.ext.iron.{Size, ValidInt}

import cats.Show
import cats.derived.*
import io.circe.Codec
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.all.*

object checkout:
  type CardNameRgx = "^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"

  opaque type CardName = String :| Match[CardNameRgx]
  object CardName extends RefinedTypeOps[String, Match[CardNameRgx], CardName]
  opaque type CardNumber = Long :| Size[16]
  object CardNumber extends RefinedTypeOps[Long, Size[16], CardNumber]
  opaque type CardExpiration = String :| (FixedLength[4] & ValidInt)
  object CardExpiration extends RefinedTypeOps[String, (FixedLength[4] & ValidInt), CardExpiration]
  opaque type CardCVV = Int :| Size[3]
  object CardCVV extends RefinedTypeOps[Int, Size[3], CardCVV]

  case class Card(
      name: CardName,
      number: CardNumber,
      expiration: CardExpiration,
      cvv: CardCVV
  ) derives Codec.AsObject, Show

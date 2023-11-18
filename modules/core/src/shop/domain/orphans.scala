package shop.domain

import cats.syntax.all.*
import cats.{Eq, Monoid, Show}
import dev.profunktor.auth.jwt.JwtToken
import io.circe.{Decoder, Encoder}
import squants.market.{Currency, INR, Money}

object OrphanInstances:
  given Eq[JwtToken] = Eq.by(_.value)
  given Show[JwtToken] = Show[String].contramap[JwtToken](_.value)
  given Eq[Currency] = Eq.and(Eq.and(Eq.by(_.code), Eq.by(_.symbol)), Eq.by(_.name))
  given Encoder[Money] = Encoder[BigDecimal].contramap(_.amount)
  given Decoder[Money] = Decoder[BigDecimal].map(INR.apply)
  given Show[Money] = Show.fromToString
  given Eq[Money] = Eq.and(Eq.by(_.amount), Eq.by(_.currency))
  given Monoid[Money] with
    def empty: Money = INR(0)
    def combine(x: Money, y: Money): Money = x + y

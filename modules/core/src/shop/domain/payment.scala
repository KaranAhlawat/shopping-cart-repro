package shop.domain

import shop.domain.auth.UserId
import shop.domain.checkout.Card

import cats.Show
import cats.derived.*
import io.circe.Encoder
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.circe.given
import squants.market.Money

object payment:
  case class Payment(id: UserId, total: Money, card: Card) derives Encoder.AsObject, Show

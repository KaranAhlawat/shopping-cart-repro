package shop.domain

import shop.domain.brand.*
import shop.domain.cart.{CartItem, Quantity}
import shop.domain.category.*
import shop.domain.given
import shop.ext.iron.*

import cats.Show
import cats.derived.*
import io.circe.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.all.*
import squants.market.{INR, Money}

import java.util.UUID

object item:
  opaque type ItemId = UUID :| Pure
  object ItemId extends RefinedTypeOps[UUID, Pure, ItemId]
  opaque type ItemName = String :| Pure
  object ItemName extends RefinedTypeOps[String, Pure, ItemName]
  opaque type ItemDescription = String :| Pure
  object ItemDescription extends RefinedTypeOps[String, Pure, ItemDescription]

  case class Item(
      uuid: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brand: Brand,
      category: Category
  ) derives Encoder.AsObject, Show:
    def cart(q: Quantity): CartItem =
      CartItem(this, q)

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brandId: BrandId,
      categoryId: CategoryId
  )

  opaque type ItemNameParam = String :| ![Blank]
  opaque type ItemDescriptionParam = String :| ![Blank]
  opaque type PriceParam = String :| ValidBigDecimal

  case class CreateItemParam(
      name: ItemNameParam,
      description: ItemDescriptionParam,
      price: PriceParam,
      brandId: BrandId,
      categoryId: CategoryId
  ) derives Decoder:
    def toDomain: CreateItem = CreateItem(
      ItemName(name),
      ItemDescription(description),
      INR(BigDecimal(price)),
      brandId,
      categoryId
    )

  case class UpdateItem(id: ItemId, price: Money)

  opaque type ItemIdParam = String :| ValidUUID

  case class UpdateItemParam(id: ItemIdParam, price: PriceParam) derives Decoder:
    def toDomain: UpdateItem = UpdateItem(
      ItemId(UUID.fromString(id).refine),
      INR(BigDecimal(price))
    )

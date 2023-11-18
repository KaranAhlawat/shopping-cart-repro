package shop.domain

import cats.Show
import cats.derived.*
import io.circe.Encoder
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.string.Blank

import java.util.UUID

object category:
  opaque type CategoryId = UUID :| Pure
  object CategoryId extends RefinedTypeOps[UUID, Pure, CategoryId]
  opaque type CategoryName = String :| Pure
  object CategoryName extends RefinedTypeOps[String, Pure, CategoryName]

  opaque type CategoryParam = String :| Not[Blank]
  object CategoryParam extends RefinedTypeOps[String, Not[Blank], CategoryParam]
  extension (x: CategoryParam)
    def toDomain: CategoryName = CategoryName(x.toLowerCase.capitalize.refine)

  case class Category(uuid: CategoryId, name: CategoryName) derives Encoder.AsObject, Show

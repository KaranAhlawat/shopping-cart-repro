package shop.domain

import cats.Show
import cats.derived.*
import io.circe.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.all.*

import java.util.UUID

object brand:
  opaque type BrandId = UUID :| Pure
  object BrandId extends RefinedTypeOps[UUID, Pure, BrandId]

  opaque type BrandName = String :| Pure
  object BrandName extends RefinedTypeOps[String, Pure, BrandName]

  opaque type BrandParam = String :| Not[Blank]
  object BrandParam extends RefinedTypeOps[String, Not[Blank], BrandParam]
  extension (x: BrandParam)
    def toDomain: BrandName = BrandName(x.toLowerCase.capitalize.refine)

  case class Brand(uuid: BrandId, name: BrandName) derives Encoder.AsObject, Show

package test.shop.domain

import shop.domain.brand.BrandId
import shop.domain.healthcheck.Status
import shop.generators.brandIdGen
import shop.optics.IsUUID

import io.github.iltotore.iron.cats.given
import monocle.law.discipline.IsoTests
import org.scalacheck.{Arbitrary, Cogen, Gen}
import weaver.FunSuite
import weaver.discipline.Discipline

import java.util.UUID

object OpticsSuite extends FunSuite with Discipline:
  given Arbitrary[Status] =
    Arbitrary(Gen.oneOf(Status.Okay, Status.Unreachable))
  given Arbitrary[BrandId] = Arbitrary(brandIdGen)
  given Cogen[BrandId] = Cogen[UUID].contramap[BrandId](_.value)

  checkAll("Iso[Status._Bool]", IsoTests(Status._Bool))
  checkAll("IsUUID[UUID]", IsoTests(IsUUID[UUID]._UUID))
  checkAll("IsUUID[BrandId]", IsoTests(IsUUID[BrandId]._UUID))

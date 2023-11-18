package test.shop.domain

import weaver.FunSuite
import weaver.discipline.Discipline
import org.scalacheck.Arbitrary
import squants.market.Money
import shop.generators.moneyGen
import cats.kernel.laws.discipline.MonoidTests
import shop.domain.given

object OrphanSuite extends FunSuite with Discipline:
  given Arbitrary[Money] = Arbitrary(moneyGen)

  checkAll("Monoid[Money]", MonoidTests[Money].monoid)

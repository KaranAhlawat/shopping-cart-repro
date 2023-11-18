package test.shop.http.routes

import shop.domain.ID
import shop.domain.brand.*
import shop.domain.item.*
import shop.ext.http4s.iron.given
import shop.generators.*
import shop.http.routes.ItemRoutes
import shop.services.Items

import cats.effect.*
import cats.syntax.all.*
import io.github.iltotore.iron.cats.given
import org.http4s.Method.*
import org.http4s.*
import org.http4s.client.dsl.io.*
import org.http4s.syntax.literals.*
import org.scalacheck.Gen
import suite.HttpSuite

object ItemRoutesSuite extends HttpSuite:
  def dataItems(items: List[Item]): Items[IO] =
    new TestItems:
      override def findAll: IO[List[Item]] =
        items.pure
      override def findBy(brand: BrandName): IO[List[Item]] =
        items.find(_.brand.name === brand).toList.pure

  test("GET item by brands succeeds"):
    val gen =
      for
        it <- Gen.listOf(itemGen)
        b <- brandGen
      yield (it, b)

    forall(gen): (it, b) =>
      val req = GET(
        uri"/items".withQueryParam("brand", b.name)
      )
      val routes = new ItemRoutes[IO](dataItems(it)).routes
      val expected = it.find(_.brand.name === b.name).toList
      expectHttpBodyAndStatus(routes, req)(expected, Status.Ok)

protected class TestItems extends Items[IO]:
  def findAll: IO[List[Item]] = List.empty.pure
  def findBy(brand: BrandName): IO[List[Item]] = List.empty.pure
  def findById(itemId: ItemId): IO[Option[Item]] = none[Item].pure
  def create(item: CreateItem): IO[ItemId] = ID.make[IO, ItemId]
  def update(item: UpdateItem): IO[Unit] = IO.unit

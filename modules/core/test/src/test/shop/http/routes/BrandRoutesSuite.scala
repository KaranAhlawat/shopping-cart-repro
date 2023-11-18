package test.shop.http.routes

import shop.domain.brand.{Brand, BrandId, BrandName}
import shop.generators.*
import shop.http.routes.BrandRoutes
import shop.services.Brands

import cats.effect.IO
import cats.implicits.*
import org.http4s.Method.*
import org.http4s.*
import org.http4s.client.dsl.io.*
import org.http4s.implicits.*
import org.scalacheck.Gen
import suite.HttpSuite

object BrandRoutesSuite extends HttpSuite:
  def dataBrands(brands: List[Brand]): Brands[IO] =
    new TestBrands:
      override def findAll: IO[List[Brand]] = IO.pure(brands)

  test("GET brands succeeds"):
    forall(Gen.listOf(brandGen)): b =>
      val req = GET(uri"/brands")
      val routes = new BrandRoutes[IO](dataBrands(b)).routes
      expectHttpBodyAndStatus(routes, req)(b, Status.Ok)

protected class TestBrands() extends Brands[IO]:
  override def findAll: IO[List[Brand]] = ???
  override def create(name: BrandName): IO[BrandId] = ???

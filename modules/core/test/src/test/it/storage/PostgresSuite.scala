package test.it.storage

import shop.domain.brand.BrandId
import shop.domain.category.CategoryId
import shop.domain.given
import shop.domain.item.CreateItem
import shop.generators.*
import shop.services.*

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import io.github.iltotore.iron.cats.given
import natchez.Trace.Implicits.noop
import org.scalacheck.Gen
import skunk.*
import skunk.implicits.sql
import suite.ResourceSuite

object PostgresSuite extends ResourceSuite:
  val flushTables: List[Command[Void]] =
    List(
      "items",
      "brands",
      "categories",
      "orders",
      "users"
    ).map(table =>
      sql"DELETE FROM #${table}".command
    )

  type Res = Resource[IO, Session[IO]]

  override def sharedResource: Resource[IO, Res] =
    Session
      .pooled[IO](
        host = "localhost",
        port = 5432,
        user = "postgres",
        password = "postgres".some,
        database = "store",
        max = 10
      )
      .beforeAll:
        _.use(s => flushTables.traverse_(s.execute))

  test("Brands"): postgres =>
    forall(brandGen): brand =>
      val b = Brands.make[IO](postgres)
      for
        x <- b.findAll
        _ <- b.create(brand.name)
        y <- b.findAll
        z <- b.create(brand.name).attempt
      yield expect.all(
        x.isEmpty,
        y.count(_.name === brand.name) === 1,
        z.isLeft
      )

  test("Categories"): postgres =>
    forall(categoryGen): category =>
      val c = Categories.make[IO](postgres)
      for
        x <- c.findAll
        _ <- c.create(category.name)
        y <- c.findAll
        z <- c.create(category.name).attempt
      yield expect.all(
        x.isEmpty,
        y.count(_.name === category.name) === 1,
        z.isLeft
      )

  test("Items"): postgres =>
    forall(itemGen): item =>
      def newItem(
          bid: Option[BrandId],
          cid: Option[CategoryId]
      ) = CreateItem(
        name = item.name,
        description = item.description,
        price = item.price,
        brandId = bid.getOrElse(item.brand.uuid),
        categoryId = cid.getOrElse(item.category.uuid)
      )

      val b = Brands.make[IO](postgres)
      val c = Categories.make[IO](postgres)
      val i = Items.make[IO](postgres)

      for
        x <- i.findAll
        _ <- b.create(item.brand.name)
        d <- b.findAll.map(_.headOption.map(_.uuid))
        _ <- c.create(item.category.name)
        e <- c.findAll.map(_.headOption.map(_.uuid))
        _ <- i.create(newItem(d, e))
        y <- i.findAll
      yield expect.all(
        x.isEmpty,
        y.count(_.name === item.name) === 1
      )

  test("Users"): postgres =>
    val gen =
      for
        u <- userNameGen
        p <- encryptedPasswordGen
      yield (u, p)

    forall(gen): (username, password) =>
      val u = Users.make[IO](postgres)
      for
        d <- u.create(username, password)
        x <- u.find(username)
        z <- u.create(username, password).attempt
      yield expect.all(
        x.count(_.id === d) === 1,
        z.isLeft
      )

  test("Orders"): postgres =>
    val itemsGen =
      Gen.nonEmptyListOf(cartItemGen)
        .map(NonEmptyList.fromListUnsafe)

    val gen =
      for
        oid <- orderIdGen
        pid <- paymentIdGen
        un <- userNameGen
        pw <- encryptedPasswordGen
        it <- itemsGen
        pr <- moneyGen
      yield (oid, pid, un, pw, it, pr)

    forall(gen): (oid, pid, un, pw, it, pr) =>
      val o = Orders.make[IO](postgres)
      val u = Users.make[IO](postgres)

      for
        d <- u.create(un, pw)
        x <- o.findBy(d)
        y <- o.get(d, oid)
        i <- o.create(d, pid, it, pr)
      yield expect.all(
        x.isEmpty,
        y.isEmpty,
        i.value.version === 4
      )

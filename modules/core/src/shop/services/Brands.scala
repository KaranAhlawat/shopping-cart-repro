package shop.services
import shop.domain.ID
import shop.domain.brand.*
import shop.effect.GenUUID
import shop.sql.codecs.*

import cats.effect.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.syntax.all.*
import skunk.*
import skunk.implicits.*

trait Brands[F[_]]:
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[BrandId]

object Brands:
  def make[F[_]: GenUUID: MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): Brands[F] =
    new Brands[F]:
      import BrandSQL.*
      override def findAll: F[List[Brand]] =
        postgres.use(_.execute(selectAll))

      override def create(name: BrandName): F[BrandId] =
        postgres.use: session =>
          session.prepare(insertBrand).flatMap: cmd =>
            ID.make[F, BrandId].flatMap: id =>
              cmd.execute(Brand(id, name)).as(id)

private object BrandSQL:
  val codec: Codec[Brand] =
    (brandId *: brandName).imap((i, n) => Brand(i, n))(b => (b.uuid, b.name))

  val selectAll: Query[Void, Brand] =
    sql"""
    SELECT * FROM brands
    """.query(codec)

  val insertBrand: Command[Brand] =
    sql"""
    INSERT INTO brands
    VALUES ($codec)
    """.command

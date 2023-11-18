package shop.services
import shop.domain.ID
import shop.domain.category.*
import shop.effect.GenUUID
import shop.sql.codecs.*

import cats.effect.kernel.Resource
import cats.syntax.all.*
import skunk.*
import skunk.implicits.*

trait Categories[F[_]]:
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[CategoryId]

object Categories:
  def make[F[_]: GenUUID: cats.effect.MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): Categories[F] =
    new Categories[F]:
      import CategorySQL.*

      override def findAll: F[List[Category]] =
        postgres.use(_.execute(selectAll))

      override def create(name: CategoryName): F[CategoryId] =
        postgres.use: session =>
          session.prepare(insertCategory).flatMap: cmd =>
            ID.make[F, CategoryId].flatMap: id =>
              cmd.execute(Category(id, name)).as(id)

private object CategorySQL:
  val codec: Codec[Category] =
    (categoryId *: categoryName).imap((i, n) => Category(i, n))(c => (c.uuid, c.name))

  val selectAll: Query[Void, Category] =
    sql"""
    SELECT * FROM categories
    """.query(codec)

  val insertCategory: Command[Category] =
    sql"""
    INSERT INTO categories
    VALUES ($codec)
    """.command

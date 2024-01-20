package shop.ext.http4s

import cats.MonadThrow
import cats.syntax.all.*
import io.circe.Decoder
import io.github.iltotore.iron.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

object iron:
  inline given [T](using
      mirror: RefinedTypeOps.Mirror[T],
      baseDecoder: QueryParamDecoder[mirror.BaseType],
      constraint: Constraint[mirror.BaseType, mirror.ConstraintType]
  ): QueryParamDecoder[T] = baseDecoder.emap: base =>
    Either.cond(
      constraint.test(base),
      base.asInstanceOf[T],
      ParseFailure(constraint.message, constraint.message)
    )

  inline given [T](using
      mirror: RefinedTypeOps.Mirror[T],
      baseEncoder: QueryParamEncoder[mirror.BaseType]
  ): QueryParamEncoder[T] =
    baseEncoder.asInstanceOf[QueryParamEncoder[T]]

  extension [F[_]: JsonDecoder: MonadThrow](req: Request[F])
    def decodeR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] =
      val dsl = Http4sDsl[F]
      import dsl.*

      req
        .asJsonDecode[A]
        .attempt
        .flatMap:
          case Right(a) =>
            f(a)
          case Left(e) =>
            Option(e.getCause()) match
              case Some(c) =>
                BadRequest(c.getMessage())
              case _ =>
                UnprocessableEntity()

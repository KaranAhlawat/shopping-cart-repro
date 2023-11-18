package shop.ext

import io.github.iltotore.iron.*
import io.github.iltotore.iron.compileTime.*

import scala.quoted.*
import scala.util.Try

object iron:
  final class ValidBigDecimal

  object ValidBigDecimal:
    inline given Constraint[String, ValidBigDecimal] with
      override inline def message: String = "Should be a valid decimal"

      override inline def test(value: String): Boolean =
        ${ check('value) }

    private def check(expr: Expr[String])(using Quotes): Expr[Boolean] =
      expr.value match
        case Some(value) => Expr(Try(BigDecimal(value)).isSuccess)
        case _           => '{ Try(BigDecimal($expr)).isSuccess }

  final class ValidInt

  object ValidInt:
    inline given Constraint[String, ValidInt] with
      override inline def message: String = "Should be a valid integer"

      override inline def test(value: String): Boolean =
        ${ check('value) }

    private def check(expr: Expr[String])(using Quotes): Expr[Boolean] =
      expr.value match
        case Some(value) => Expr(value.toIntOption.isDefined)
        case _           => '{ $expr.toIntOption.isDefined }

  final class Size[V]

  object Size:
    private trait SizeConstraint[A, V <: NumConstant] extends Constraint[A, Size[V]]:
      override inline def message: String = "Should be of " + stringValue[V] + " digits"

    inline given [V <: Int | Long]: SizeConstraint[Long, V] with
      override inline def test(value: Long): Boolean =
        ${ checkLong('value, '{ longValue[V] }) }

    private def checkLong(expr: Expr[Long], size: Expr[Long])(using Quotes): Expr[Boolean] =
      (expr.value, size.value) match
        case (Some(l), Some(s)) => Expr(l.toString.length == s)
        case _                  => '{ $expr.toString.length == $size }

    inline given [V <: Int | Long]: SizeConstraint[Int, V] with
      override inline def test(value: Int): Boolean =
        ${ checkInt('value, '{ longValue[V] }) }

    private def checkInt(expr: Expr[Int], size: Expr[Long])(using Quotes): Expr[Boolean] =
      (expr.value, size.value) match
        case (Some(l), Some(s)) => Expr(l.toString.length == s)
        case _                  => '{ $expr.toString.length == $size }

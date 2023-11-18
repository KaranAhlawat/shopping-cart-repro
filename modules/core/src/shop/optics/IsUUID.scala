package shop.optics

import shop.domain.brand.*

import io.github.iltotore.iron.RefinedTypeOps
import monocle.Iso

import scala.annotation.implicitNotFound

import java.util.UUID

@implicitNotFound("Only newtype UUID-based instances can be derived, except java.util.UUID")
trait IsUUID[A]:
  def _UUID: Iso[UUID, A]

object IsUUID:
  def apply[A](using ev: IsUUID[A]): IsUUID[A] = ev

  inline given [T](using ev: RefinedTypeOps.Mirror[T], bt: ev.BaseType =:= UUID): IsUUID[T] with
    def _UUID = Iso[UUID, ev.FinalType](_.asInstanceOf[ev.FinalType])(_.asInstanceOf[UUID])

  given IsUUID[UUID] with
    def _UUID = Iso[UUID, UUID](identity)(identity)

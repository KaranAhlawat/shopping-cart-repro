package shop.http.clients

import shop.config.types.PaymentConfig
import shop.domain.order.OrderOrPaymentError.*
import shop.domain.order.*
import shop.domain.payment.*

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import org.http4s.Method.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

trait PaymentClient[F[_]]:
  def process(payment: Payment): F[PaymentId]

object PaymentClient:
  def make[F[_]: JsonDecoder: MonadCancelThrow](
      cfg: PaymentConfig,
      client: Client[F]
  ): PaymentClient[F] =
    new PaymentClient[F] with Http4sClientDsl[F]:
      def process(payment: Payment): F[PaymentId] =
        Uri
          .fromString(cfg.uri.value + "/payments")
          .liftTo[F]
          .flatMap: uri =>
            client.run(POST(payment, uri)).use: resp =>
              resp.status match
                case Status.Ok | Status.Conflict =>
                  resp.asJsonDecode[PaymentId]
                case st =>
                  PaymentError(
                    Option(st.reason).getOrElse("unknown")
                  ).raiseError[F, PaymentId]

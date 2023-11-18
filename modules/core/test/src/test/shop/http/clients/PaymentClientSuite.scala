package test.shop.http.clients

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.io.*
import shop.domain.order.PaymentId
import org.http4s.client.Client
import org.http4s.circe.CirceEntityEncoder.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.cats.given
import shop.generators.*
import shop.config.types.{PaymentURI, PaymentConfig}
import shop.http.clients.PaymentClient
import shop.domain.order.OrderOrPaymentError.PaymentError

object PaymentClientSuite extends SimpleIOSuite with Checkers:
  def routes(mkResponse: IO[Response[IO]]) =
    HttpRoutes
      .of[IO]:
        case POST -> Root / "payments" => mkResponse
      .orNotFound

  val config = PaymentConfig(PaymentURI("http://localhost"))

  val gen =
    for
      i <- paymentIdGen
      p <- paymentGen
    yield (i, p)

  test("Response Ok (200)"):
    forall(gen): (pid, payment) =>
      val client = Client.fromHttpApp(routes(Ok(pid)))

      PaymentClient
        .make[IO](config, client)
        .process(payment)
        .map(expect.same(pid, _))

  test("Response Conflict (409)"):
    forall(gen): (pid, payment) =>
      val client = Client.fromHttpApp(routes(Conflict(pid)))

      PaymentClient
        .make[IO](config, client)
        .process(payment)
        .map(expect.same(pid, _))

  test("Response Internal Server Error (500)"):
    forall(paymentGen): payment =>
      val client = Client.fromHttpApp(routes(InternalServerError()))

      PaymentClient
        .make[IO](config, client)
        .process(payment)
        .attempt
        .map:
          case Left(e) =>
            expect.same(PaymentError("Internal Server Error"), e)

          case Right(_) =>
            failure("expected payment error")

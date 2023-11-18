package suite

import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.{Status as HttpStatus, *}
import org.http4s.circe.*
import weaver.scalacheck.Checkers
import weaver.{Expectations, SimpleIOSuite}

trait HttpSuite extends SimpleIOSuite with Checkers:
  def expectHttpBodyAndStatus[A: Encoder](
      routes: HttpRoutes[IO],
      req: Request[IO]
  )(
      expectedBody: A,
      expectedStatus: HttpStatus
  ): IO[Expectations] =
    routes.run(req).value.flatMap:
      case Some(resp) =>
        resp.asJson.map: json =>
          expect.same(resp.status, expectedStatus) |+|
            expect.same(
              json.dropNullValues,
              expectedBody.asJson.dropNullValues
            )
      case None => IO.pure(failure("route not found"))

  def expectHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(expectedStatus: HttpStatus): IO[Expectations] =
    routes.run(req).value.map:
      case Some(resp) => expect.same(resp.status, expectedStatus)
      case _          => failure("route not found")

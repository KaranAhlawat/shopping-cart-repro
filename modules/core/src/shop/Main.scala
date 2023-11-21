package shop

import shop.config.Config
import shop.modules.*
import shop.resources.{AppResources, MkHttpServer}

import cats.effect.std.Supervisor
import cats.effect.{IO, IOApp}
import dev.profunktor.redis4cats.log4cats.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple:
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    Config.load[IO].flatMap: cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use:
          case given Supervisor[IO] =>
            AppResources
              .make[IO](cfg)
              .evalMap: res =>
                Security.make[IO](
                  cfg,
                  res.postgres,
                  res.redis
                )
                  .map: security =>
                    val clients = HttpClients.make[IO](
                      cfg.paymentConfig,
                      res.client
                    )
                    val services = Services.make[IO](
                      res.redis,
                      res.postgres,
                      cfg.cartExpiration
                    )
                    val programs = Programs.make[IO](
                      cfg.checkoutConfig,
                      services,
                      clients
                    )
                    val api = HttpApi.make[IO](services, programs, security)
                    (cfg.httpServerConfig, api.httpApp)
              .flatMap: (cfg, httpApp) =>
                MkHttpServer[IO].newEmber(cfg, httpApp)
              .useForever

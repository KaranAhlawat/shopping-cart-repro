package shop.config

import cats.syntax.all.*
import ciris.{ConfigDecoder, ConfigError}

enum AppEnvironment:
  case Test
  case Prod

object AppEnvironment:
  given ConfigDecoder[String, AppEnvironment] =
    ConfigDecoder[String, String]
      .mapEither: (_, value) =>
        value match
          case "test" => AppEnvironment.Test.asRight
          case "prod" => AppEnvironment.Prod.asRight
          case _      => ConfigError(s"Invalid SC_APP_ENV variable set: ${value}").asLeft

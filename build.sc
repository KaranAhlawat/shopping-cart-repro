import $file.deps
import $ivy.`com.goyeau::mill-scalafix_mill0.11:0.3.1`
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.scalalib._

object modules extends Module {
  object core extends ScalaModule with ScalafixModule {
    def scalaVersion = "3.3.1"

    def scalacOptions = T {
      super.scalacOptions() ++ deps.compilerOptions
    }

    def ivyDeps = Agg(
      ivy"org.typelevel::cats-core:${deps.catsVersion}",
      ivy"org.typelevel::cats-effect:${deps.catsEffectVersion}",
      ivy"org.typelevel::kittens:${deps.kittensVersion}",
      ivy"org.typelevel::squants:${deps.squantsVersion}",
      ivy"org.typelevel::cats-laws:${deps.catsVersion}",
      ivy"io.github.iltotore::iron:${deps.ironVersion}",
      ivy"io.github.iltotore::iron-circe:${deps.ironVersion}",
      ivy"io.github.iltotore::iron-cats:${deps.ironVersion}",
      ivy"io.github.iltotore::iron-ciris:${deps.ironVersion}",
      ivy"dev.optics::monocle-core:${deps.monocleVersion}",
      ivy"dev.optics::monocle-law:${deps.monocleVersion}",
      ivy"com.github.cb372::cats-retry:${deps.catsRetryVersion}",
      ivy"org.typelevel::log4cats-slf4j:${deps.log4CatsVersion}",
      ivy"org.http4s::http4s-dsl:${deps.http4sVersion}",
      ivy"org.http4s::http4s-ember-server:${deps.http4sVersion}",
      ivy"org.http4s::http4s-ember-client:${deps.http4sVersion}",
      ivy"org.http4s::http4s-circe:${deps.http4sVersion}",
      ivy"dev.profunktor::http4s-jwt-auth:${deps.http4sJwtAuthVersion}",
      ivy"io.circe::circe-core:${deps.circeVersion}",
      ivy"io.circe::circe-generic:${deps.circeVersion}",
      ivy"io.circe::circe-parser:${deps.circeVersion}",
      ivy"org.tpolecat::skunk-core:${deps.skunkVersion}",
      ivy"org.tpolecat::skunk-circe:${deps.skunkVersion}",
      ivy"dev.profunktor::redis4cats-effects:${deps.redis4CatsVersion}",
      ivy"dev.profunktor::redis4cats-log4cats:${deps.redis4CatsVersion}",
      ivy"is.cir::ciris:${deps.cirisVersion}",

      // Runtime
      ivy"ch.qos.logback:logback-classic:${deps.logbackVersion}"
    )

    object test extends ScalaTests with ScalafixModule {
      def ivyDeps = Agg(
        ivy"com.disneystreaming::weaver-cats:${deps.weaverVersion}",
        ivy"com.disneystreaming::weaver-discipline:${deps.weaverVersion}",
        ivy"com.disneystreaming::weaver-scalacheck:${deps.weaverVersion}",
        ivy"org.typelevel::log4cats-noop:${deps.log4CatsNoOpVersion}"
      )

      def testFramework = "weaver.framework.CatsEffect"
    }
  }
}

alias c := core-compile
alias r := core-run
alias t := core-test
alias test := core-test-one

runner := "mill --disable-ticker"

core-run:
  {{runner}} -i modules.core.run

core-compile:
  {{runner}} modules.core.compile

core-test:
  {{runner}} modules.core.test

core-test-one TEST:
  {{runner}} -w modules.core.test {{TEST}}

core-fix:
  {{runner}} modules.core.fix 

format-all:
  {{runner}} mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources


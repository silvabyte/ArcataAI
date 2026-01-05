package build

import mill._, scalalib._
import mill.scalalib.scalafmt.ScalafmtModule

object api extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.7.2"

  // Use Maven-style layout - millSourcePath is <root>/api, but sources are at <root>/src
  def sources = Task.Sources(millSourcePath / os.up / "src" / "main" / "scala")
  def resources = Task.Sources(millSourcePath / os.up / "src" / "main" / "resources")

  def ivyDeps = Agg(
    // HTTP server
    ivy"com.lihaoyi::cask:0.9.7",
    // JSON serialization
    ivy"com.lihaoyi::upickle:4.1.0",
    // File system operations
    ivy"com.lihaoyi::os-lib:0.11.3",
    // HTTP client (for API calls)
    ivy"com.lihaoyi::requests:0.9.0",
    // Logging
    ivy"com.outr::scribe:3.6.6",
    // BoogieLoops schema + web for OpenAPI generation and validation
    ivy"dev.boogieloop::schema:0.6.0",
    ivy"dev.boogieloop::web:0.6.0",
    // BoogieLoops AI for LLM extraction
    ivy"dev.boogieloop::ai:0.6.0",
    // BoogieLoops Kit for file type detection
    ivy"dev.boogieloop::kit:0.6.0",
    // JWT validation (for Supabase JWTs)
    ivy"com.auth0:java-jwt:4.4.0",
    // HTML parsing and cleaning
    ivy"org.jsoup:jsoup:1.17.2",
    // HTML to Markdown conversion
    ivy"com.vladsch.flexmark:flexmark-html2md-converter:0.64.8",
    // Actor framework for async workflows
    ivy"com.lihaoyi::castor:0.3.0",
    // PDF text extraction (minimal deps, no logging framework conflicts)
    ivy"com.github.librepdf:openpdf:2.0.3"
  )

  def scalacOptions = Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:implicitConversions"
  )

  def mainClass = Some("arcata.api.ApiApp")

  // Scalafix configuration
  private def scalafixConfig = millSourcePath / os.up / ".scalafix.conf"

  private def sourceFiles: Seq[String] = {
    val srcDir = millSourcePath / os.up / "src" / "main" / "scala"
    if (os.exists(srcDir)) {
      os.walk(srcDir)
        .filter(_.ext == "scala")
        .map(_.toString)
    } else {
      Seq.empty
    }
  }

  /** Run Scalafix to auto-fix linting issues */
  def fix() = Task.Command {
    val files = sourceFiles
    if (files.nonEmpty) {
      os.proc(
        "cs",
        "launch",
        "scalafix",
        "--",
        "--config",
        scalafixConfig.toString,
        "--scala-version",
        scalaVersion(),
        "--syntactic",
        files
      ).call(cwd = millSourcePath / os.up)
    }
    ()
  }

  /** Check Scalafix rules without making changes */
  def fixCheck() = Task.Command {
    val files = sourceFiles
    if (files.nonEmpty) {
      os.proc(
        "cs",
        "launch",
        "scalafix",
        "--",
        "--config",
        scalafixConfig.toString,
        "--scala-version",
        scalaVersion(),
        "--syntactic",
        "--check",
        files
      ).call(cwd = millSourcePath / os.up)
    }
    ()
  }

  object test extends ScalaTests with ScalafmtModule {
    def sources = Task.Sources(api.millSourcePath / os.up / "src" / "test" / "scala")
    def resources = Task.Sources(api.millSourcePath / os.up / "src" / "test" / "resources")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest:0.8.5",
      ivy"org.scalamock::scalamock:7.1.0",
      // Undertow for test server
      ivy"io.undertow:undertow-core:2.3.18.Final"
    )
    def testFramework = "utest.runner.Framework"

    // Scalafix for test sources
    private def testSourceFiles: Seq[String] = {
      val srcDir = api.millSourcePath / os.up / "src" / "test" / "scala"
      if (os.exists(srcDir)) {
        os.walk(srcDir)
          .filter(_.ext == "scala")
          .map(_.toString)
      } else {
        Seq.empty
      }
    }

    def fix() = Task.Command {
      val files = testSourceFiles
      if (files.nonEmpty) {
        os.proc(
          "cs",
          "launch",
          "scalafix",
          "--",
          "--config",
          (api.millSourcePath / os.up / ".scalafix.conf").toString,
          "--scala-version",
          api.scalaVersion(),
          "--syntactic",
          files
        ).call(cwd = api.millSourcePath / os.up)
      }
      ()
    }

    def fixCheck() = Task.Command {
      val files = testSourceFiles
      if (files.nonEmpty) {
        os.proc(
          "cs",
          "launch",
          "scalafix",
          "--",
          "--config",
          (api.millSourcePath / os.up / ".scalafix.conf").toString,
          "--scala-version",
          api.scalaVersion(),
          "--syntactic",
          "--check",
          files
        ).call(cwd = api.millSourcePath / os.up)
      }
      ()
    }
  }
}

package arcata.api.extraction

import utest.*
import arcata.api.domain.{ExtractionConfig, MatchPattern, MatchPatternType, ExtractionRule, ExtractionSource}

object ConfigMatcherSuite extends TestSuite:

  // Sample HTML for testing
  val htmlWithJsonLd = """
                         |<!DOCTYPE html>
                         |<html>
                         |<head>
                         |  <title>Software Engineer - Netflix</title>
                         |  <script type="application/ld+json">
                         |  {
                         |    "@context": "https://schema.org/",
                         |    "@type": "JobPosting",
                         |    "title": "Software Engineer",
                         |    "description": "Great job opportunity"
                         |  }
                         |  </script>
                         |</head>
                         |<body>
                         |  <h1>Software Engineer</h1>
                         |  <div class="company">Netflix</div>
                         |</body>
                         |</html>
  """.stripMargin

  val htmlWithoutJsonLd = """
                            |<!DOCTYPE html>
                            |<html>
                            |<head>
                            |  <title>Software Engineer - Acme Corp</title>
                            |</head>
                            |<body>
                            |  <h1 class="job-title">Software Engineer</h1>
                            |  <div class="company-name">Acme Corp</div>
                            |</body>
                            |</html>
  """.stripMargin

  // Create test configs
  val jsonLdConfig = ExtractionConfig.create(
    name = "Schema.org JSON-LD",
    matchPatterns = Seq(
      MatchPattern.cssExists("script[type='application/ld+json']", Some("JobPosting"))
    ),
    extractRules = Map(
      "title" -> Seq(ExtractionRule.jsonLd("$.title"))
    )
  )

  val cssConfig = ExtractionConfig.create(
    name = "CSS Selectors",
    matchPatterns = Seq(
      MatchPattern.cssExists(".job-title"),
      MatchPattern.cssExists(".company-name")
    ),
    extractRules = Map(
      "title" -> Seq(ExtractionRule.css(".job-title"))
    )
  )

  val urlConfig = ExtractionConfig.create(
    name = "Netflix Careers",
    matchPatterns = Seq(
      MatchPattern.urlPattern(".*\\.jobs\\.netflix\\.net/careers/job/.*")
    ),
    extractRules = Map(
      "title" -> Seq(ExtractionRule.css("h1"))
    )
  )

  val contentConfig = ExtractionConfig.create(
    name = "Contains JobPosting",
    matchPatterns = Seq(
      MatchPattern.contentContains("JobPosting")
    ),
    extractRules = Map(
      "title" -> Seq(ExtractionRule.css("h1"))
    )
  )

  val tests = Tests {

    test("CssExists matches when selector exists") {
      val result = ConfigMatcher.findMatch(htmlWithJsonLd, "https://example.com", Seq(jsonLdConfig))
      assert(result.isDefined)
      assert(result.get.name == "Schema.org JSON-LD")
    }

    test("CssExists with contentContains filters by content") {
      // Create a config that requires specific content in the matched element
      val strictConfig = ExtractionConfig.create(
        name = "Requires Review type",
        matchPatterns = Seq(
          MatchPattern.cssExists("script[type='application/ld+json']", Some("Review"))
        ),
        extractRules = Map.empty
      )

      val result = ConfigMatcher.findMatch(htmlWithJsonLd, "https://example.com", Seq(strictConfig))
      // Should NOT match because the JSON-LD contains JobPosting, not Review
      assert(result.isEmpty)
    }

    test("CssExists does not match when selector missing") {
      val result = ConfigMatcher.findMatch(htmlWithoutJsonLd, "https://example.com", Seq(jsonLdConfig))
      assert(result.isEmpty)
    }

    test("UrlPattern matches when URL matches regex") {
      val url = "https://explore.jobs.netflix.net/careers/job/123456"
      val result = ConfigMatcher.findMatch(htmlWithJsonLd, url, Seq(urlConfig))
      assert(result.isDefined)
      assert(result.get.name == "Netflix Careers")
    }

    test("UrlPattern does not match when URL differs") {
      val url = "https://careers.google.com/jobs/123456"
      val result = ConfigMatcher.findMatch(htmlWithJsonLd, url, Seq(urlConfig))
      assert(result.isEmpty)
    }

    test("ContentContains matches when content present") {
      val result = ConfigMatcher.findMatch(htmlWithJsonLd, "https://example.com", Seq(contentConfig))
      assert(result.isDefined)
      assert(result.get.name == "Contains JobPosting")
    }

    test("ContentContains does not match when content missing") {
      val result = ConfigMatcher.findMatch(htmlWithoutJsonLd, "https://example.com", Seq(contentConfig))
      assert(result.isEmpty)
    }

    test("all patterns must match for config to match") {
      // cssConfig requires BOTH .job-title and .company-name
      val result1 = ConfigMatcher.findMatch(htmlWithoutJsonLd, "https://example.com", Seq(cssConfig))
      assert(result1.isDefined)

      // htmlWithJsonLd doesn't have .job-title or .company-name
      val result2 = ConfigMatcher.findMatch(htmlWithJsonLd, "https://example.com", Seq(cssConfig))
      assert(result2.isEmpty)
    }

    test("most specific config wins") {
      // Create a more specific config (more patterns)
      val moreSpecificConfig = ExtractionConfig.create(
        name = "More Specific",
        matchPatterns = Seq(
          MatchPattern.cssExists("script[type='application/ld+json']", Some("JobPosting")),
          MatchPattern.contentContains("Netflix")
        ),
        extractRules = Map.empty
      )

      val htmlWithNetflix = htmlWithJsonLd.replace("Great job opportunity", "Netflix job opportunity")
      val result =
        ConfigMatcher.findMatch(htmlWithNetflix, "https://example.com", Seq(jsonLdConfig, moreSpecificConfig))

      // Should prefer moreSpecificConfig because it has more patterns
      assert(result.isDefined)
      assert(result.get.name == "More Specific")
    }

    test("findAllMatches returns all matching configs") {
      val htmlWithNetflix = htmlWithJsonLd.replace("Great job opportunity", "Netflix job opportunity")
      val results =
        ConfigMatcher.findAllMatches(htmlWithNetflix, "https://example.com", Seq(jsonLdConfig, contentConfig))

      // Both should match
      assert(results.size == 2)
    }

    test("computeHash produces consistent hashes") {
      val patterns = Seq(
        MatchPattern.cssExists("script[type='application/ld+json']"),
        MatchPattern.urlPattern(".*example\\.com.*")
      )

      val hash1 = ConfigMatcher.computeHash(patterns)
      val hash2 = ConfigMatcher.computeHash(patterns)
      val hash3 = ConfigMatcher.computeHash(patterns.reverse) // Different order

      assert(hash1 == hash2)
      // Hashes should be the same regardless of order (patterns are sorted internally)
      assert(hash1 == hash3)
    }

    test("handles malformed regex gracefully") {
      val badConfig = ExtractionConfig.create(
        name = "Bad Regex",
        matchPatterns = Seq(
          MatchPattern.urlPattern("[invalid(regex")
        ),
        extractRules = Map.empty
      )

      // Should not throw, just return no match
      val result = ConfigMatcher.findMatch(htmlWithJsonLd, "https://example.com", Seq(badConfig))
      assert(result.isEmpty)
    }

    test("handles empty configs list") {
      val result = ConfigMatcher.findMatch(htmlWithJsonLd, "https://example.com", Seq.empty)
      assert(result.isEmpty)
    }

    test("handles empty patterns list") {
      val emptyConfig = ExtractionConfig.create(
        name = "Empty Patterns",
        matchPatterns = Seq.empty,
        extractRules = Map.empty
      )

      // A config with no patterns should not match anything
      val result = ConfigMatcher.findMatch(htmlWithJsonLd, "https://example.com", Seq(emptyConfig))
      assert(result.isEmpty)
    }
  }

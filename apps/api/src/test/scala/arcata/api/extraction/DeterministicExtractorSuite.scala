package arcata.api.extraction

import utest.*
import arcata.api.domain.{ExtractionConfig, MatchPattern, ExtractionRule, ExtractionSource, Transform}

object DeterministicExtractorSuite extends TestSuite:

  // Sample HTML with JSON-LD
  val htmlWithJsonLd = """
                         |<!DOCTYPE html>
                         |<html>
                         |<head>
                         |  <title>Software Engineer - Netflix</title>
                         |  <meta name="description" content="Apply now for this exciting role!">
                         |  <script type="application/ld+json">
                         |  {
                         |    "@context": "https://schema.org/",
                         |    "@type": "JobPosting",
                         |    "title": "Software Engineer (L5), Ads Media Planning",
                         |    "description": "<p>Join our team building the next generation of ad technology.</p>",
                         |    "hiringOrganization": {
                         |      "@type": "Organization",
                         |      "name": "Netflix, Inc."
                         |    },
                         |    "jobLocation": {
                         |      "@type": "Place",
                         |      "address": {
                         |        "@type": "PostalAddress",
                         |        "addressLocality": "Los Angeles",
                         |        "addressRegion": "CA"
                         |      }
                         |    },
                         |    "baseSalary": {
                         |      "@type": "MonetaryAmount",
                         |      "currency": "USD",
                         |      "value": {
                         |        "@type": "QuantitativeValue",
                         |        "minValue": 100000,
                         |        "maxValue": 720000
                         |      }
                         |    },
                         |    "employmentType": "FULL_TIME",
                         |    "experienceRequirements": "5+ years"
                         |  }
                         |  </script>
                         |</head>
                         |<body>
                         |  <h1 class="job-title">Software Engineer</h1>
                         |  <div class="company">Netflix</div>
                         |  <div class="salary">$100,000 - $720,000</div>
                         |</body>
                         |</html>
  """.stripMargin

  // Config that extracts from JSON-LD
  val jsonLdConfig = ExtractionConfig.create(
    name = "Schema.org JSON-LD",
    matchPatterns = Seq(
      MatchPattern.cssExists("script[type='application/ld+json']", Some("JobPosting"))
    ),
    extractRules = Map(
      "title" -> Seq(ExtractionRule.jsonLd("$.title")),
      "companyName" -> Seq(ExtractionRule.jsonLd("$.hiringOrganization.name")),
      "description" -> Seq(
        ExtractionRule.jsonLd("$.description", Seq(Transform.InnerText))
      ),
      "location" -> Seq(ExtractionRule.jsonLd("$.jobLocation.address.addressLocality")),
      "salaryMin" -> Seq(ExtractionRule.jsonLd("$.baseSalary.value.minValue")),
      "salaryMax" -> Seq(ExtractionRule.jsonLd("$.baseSalary.value.maxValue")),
      "jobType" -> Seq(ExtractionRule.jsonLd("$.employmentType")),
      "experienceLevel" -> Seq(ExtractionRule.jsonLd("$.experienceRequirements"))
    )
  )

  // Config that extracts from CSS
  val cssConfig = ExtractionConfig.create(
    name = "CSS Selectors",
    matchPatterns = Seq(
      MatchPattern.cssExists(".job-title")
    ),
    extractRules = Map(
      "title" -> Seq(ExtractionRule.css(".job-title")),
      "companyName" -> Seq(ExtractionRule.css(".company")),
      "description" -> Seq(ExtractionRule.meta("description"))
    )
  )

  // Config with fallback rules
  val fallbackConfig = ExtractionConfig.create(
    name = "Fallback Rules",
    matchPatterns = Seq(MatchPattern.contentContains("job")),
    extractRules = Map(
      "title" -> Seq(
        // First try CSS that doesn't exist
        ExtractionRule.css(".nonexistent-title"),
        // Then fall back to JSON-LD
        ExtractionRule.jsonLd("$.title")
      )
    )
  )

  val tests = Tests {

    test("extracts from JSON-LD path") {
      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", jsonLdConfig)

      assert(result.data.title == "Software Engineer (L5), Ads Media Planning")
      assert(result.data.companyName.contains("Netflix, Inc."))
      assert(result.data.location.contains("Los Angeles"))
    }

    test("extracts nested JSON-LD values") {
      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", jsonLdConfig)

      // Salary values from nested path
      assert(result.data.salaryMin.contains(100000))
      assert(result.data.salaryMax.contains(720000))
    }

    test("extracts from CSS selectors") {
      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", cssConfig)

      assert(result.data.title == "Software Engineer")
      assert(result.data.companyName.contains("Netflix"))
    }

    test("extracts from meta tags") {
      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", cssConfig)

      assert(result.data.description.contains("Apply now for this exciting role!"))
    }

    test("uses fallback rules when first rule fails") {
      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", fallbackConfig)

      // Should fall back to JSON-LD since .nonexistent-title doesn't exist
      assert(result.data.title == "Software Engineer (L5), Ads Media Planning")
    }

    test("applies InnerText transform to strip HTML") {
      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", jsonLdConfig)

      // Description should have HTML stripped
      val desc = result.data.description.getOrElse("")
      assert(!desc.contains("<p>"))
      assert(desc.contains("Join our team"))
    }

    test("calculates completeness score") {
      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", jsonLdConfig)

      // Should have high score with JSON-LD extraction
      // title(20) + company(15) + description(25) + location(10) + salaryMin(5) + salaryMax(5) + jobType(3) + experienceLevel(2) = 85
      assert(result.scoringResult.earnedPoints >= 85)
      assert(
        result.scoringResult.state == CompletionState.Sufficient || result.scoringResult.state == CompletionState.Complete
      )
    }

    test("tracks failed rules for debugging") {
      // Create config with rules that will fail
      val failingConfig = ExtractionConfig.create(
        name = "Failing Config",
        matchPatterns = Seq.empty,
        extractRules = Map(
          "title" -> Seq(
            ExtractionRule.jsonLd("$.nonexistent.path")
          )
        )
      )

      val result = DeterministicExtractor.extract(htmlWithJsonLd, "https://example.com", failingConfig)

      // Should have failure recorded
      assert(result.failedRules.contains("title"))
    }

    test("handles missing JSON-LD gracefully") {
      val htmlNoJsonLd = "<html><body><h1>Job Title</h1></body></html>"

      val result = DeterministicExtractor.extract(htmlNoJsonLd, "https://example.com", jsonLdConfig)

      // Should fail gracefully, using "Unknown Title" as default
      assert(result.data.title == "Unknown Title")
      assert(result.scoringResult.state == CompletionState.Failed)
    }

    test("handles malformed HTML gracefully") {
      val badHtml = "<html><body><h1>Unclosed tag"

      // Should not throw
      val result = DeterministicExtractor.extract(badHtml, "https://example.com", cssConfig)
      assert(result.data.title == "Unknown Title" || result.data.title.nonEmpty)
    }

    test("HtmlDecode transform decodes entities") {
      val htmlWithEntities = """
                               |<html>
                               |<script type="application/ld+json">
                               |{"title": "Senior &amp; Junior Engineer"}
                               |</script>
                               |</html>
      """.stripMargin

      val decodeConfig = ExtractionConfig.create(
        name = "Decode Test",
        matchPatterns = Seq.empty,
        extractRules = Map(
          "title" -> Seq(
            ExtractionRule.jsonLd("$.title", Seq(Transform.HtmlDecode))
          )
        )
      )

      val result = DeterministicExtractor.extract(htmlWithEntities, "https://example.com", decodeConfig)
      assert(result.data.title == "Senior & Junior Engineer")
    }

    test("ParseNumber transform extracts numbers") {
      // Create HTML with a simple salary number
      val simpleSalaryHtml = """
                               |<html><body>
                               |<div class="salary">$150,000 per year</div>
                               |</body></html>
      """.stripMargin

      val salaryConfig = ExtractionConfig.create(
        name = "Salary Parse",
        matchPatterns = Seq.empty,
        extractRules = Map(
          "salaryMin" -> Seq(
            ExtractionRule.css(".salary", Seq(Transform.ParseNumber))
          )
        )
      )

      val result = DeterministicExtractor.extract(simpleSalaryHtml, "https://example.com", salaryConfig)
      // ParseNumber removes non-digits, so "$150,000 per year" becomes "150000"
      assert(result.data.salaryMin.contains(150000))
    }
  }

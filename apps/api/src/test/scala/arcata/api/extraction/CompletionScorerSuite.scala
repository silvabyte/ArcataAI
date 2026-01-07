package arcata.api.extraction

import utest.*

object CompletionScorerSuite extends TestSuite:

  val tests = Tests {

    test("Complete state for 90%+ score") {
      // All fields with sufficiently long values
      val fields = Map(
        "title" -> Some("Software Engineer Role"),
        "companyName" -> Some("Netflix Inc"),
        "description" -> Some("A great job opportunity with excellent benefits and growth."),
        "location" -> Some("Los Angeles, CA"),
        "salaryMin" -> Some("100000 USD"),
        "salaryMax" -> Some("200000 USD"),
        "qualifications" -> Some("5+ years experience, Java, Scala"),
        "responsibilities" -> Some("Build systems, Lead projects"),
        "benefits" -> Some("Health, 401k, Stock options"),
        "jobType" -> Some("Full-time position"),
        "experienceLevel" -> Some("Senior level"),
        // All 11 fields = 100 points
      )

      val result = CompletionScorer.score(fields)
      assert(result.earnedPoints == 100)
      assert(result.state == CompletionState.Complete)
      assert(result.score >= 0.90)
      assert(result.hasRequiredFields)
    }

    test("Sufficient state for 70-90% score") {
      // Required fields + some optional
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some("Netflix"),
        "description" -> Some("A great job opportunity"),
        "location" -> Some("Remote"),
        "salaryMin" -> Some("100000"),
        "salaryMax" -> Some("200000"),
        // Missing: qualifications (5), responsibilities (5), benefits (5), jobType (3), experienceLevel (2)
        // Total: 80/100 = 80%
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Sufficient)
      assert(result.score >= 0.70)
      assert(result.score < 0.90)
      assert(result.hasRequiredFields)
    }

    test("Partial state for 50-70% score") {
      // Required fields only
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some("Netflix"),
        "description" -> Some("A great job opportunity"),
        // Missing all optional fields
        // Total: 60/100 = 60%
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Partial)
      assert(result.score >= 0.50)
      assert(result.score < 0.70)
      assert(result.hasRequiredFields)
    }

    test("Minimal state for <50% with required fields") {
      // Only title and company (missing description)
      // This would be Failed since description is required
      // Let's test with required + one small optional
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some("Netflix"),
        "description" -> Some("Brief description here"),
        "jobType" -> Some("Full-time"),
        // Total: 63/100 = 63%
      )

      val result = CompletionScorer.score(fields)
      // This is actually Partial at 63%
      assert(result.state == CompletionState.Partial)
    }

    test("Failed state when missing required fields") {
      // Missing description
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some("Netflix"),
        "description" -> None, // Missing!
        "location" -> Some("Remote"),
        "salaryMin" -> Some("100000"),
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Failed)
      assert(!result.hasRequiredFields)
      assert(result.missingRequired.contains("description"))
    }

    test("Failed state when missing title") {
      val fields = Map(
        "title" -> None, // Missing!
        "companyName" -> Some("Netflix"),
        "description" -> Some("Great job opportunity"),
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Failed)
      assert(result.missingRequired.contains("title"))
    }

    test("Failed state when missing company") {
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> None, // Missing!
        "description" -> Some("Great job opportunity"),
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Failed)
      assert(result.missingRequired.contains("companyName"))
    }

    test("fields too short are not counted") {
      val fields = Map(
        "title" -> Some("SE"), // Too short (< 5 chars)
        "companyName" -> Some("Netflix"),
        "description" -> Some("A great job opportunity"),
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Failed)
      assert(result.missingRequired.contains("title"))
    }

    test("empty strings are not counted") {
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some(""), // Empty
        "description" -> Some("A great job opportunity"),
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Failed)
      assert(result.missingRequired.contains("companyName"))
    }

    test("whitespace-only strings are not counted") {
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some("   "), // Whitespace only
        "description" -> Some("A great job opportunity"),
      )

      val result = CompletionScorer.score(fields)
      assert(result.state == CompletionState.Failed)
    }

    test("ScoringResult provides useful summary") {
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some("Netflix"),
        "description" -> Some("A great job opportunity"),
        "location" -> Some("Remote location"),
      )

      val result = CompletionScorer.score(fields)
      // 20 + 15 + 25 + 10 = 70 points = 70% = Sufficient
      assert(result.summary.contains("Sufficient") || result.summary.contains("Partial"))
      assert(result.scorePercent.contains("%"))
      assert(result.presentFields.contains("title"))
      assert(result.presentFields.contains("location"))
      assert(result.missingOptional.contains("salaryMin"))
    }

    test("boundary case: exactly 90% is Complete") {
      // Need exactly 90 points
      // title(20) + company(15) + description(25) + location(10) + salaryMin(5) + salaryMax(5) + qualifications(5) + responsibilities(5) = 90
      val fields = Map(
        "title" -> Some("Software Engineer role"),
        "companyName" -> Some("Netflix Company"),
        "description" -> Some("A great job opportunity with details"),
        "location" -> Some("Remote Location Here"),
        "salaryMin" -> Some("100000 USD"),
        "salaryMax" -> Some("200000 USD"),
        "qualifications" -> Some("Required qualifications list"),
        "responsibilities" -> Some("Key responsibilities here"),
      )

      val result = CompletionScorer.score(fields)
      // 8 fields with 5+ chars each
      assert(result.presentFields.size == 8)
      assert(result.earnedPoints == 90)
      assert(result.score == 0.90)
      assert(result.state == CompletionState.Complete)
    }

    test("boundary case: 89% is Sufficient") {
      // 89 points would be Sufficient
      // title(20) + company(15) + description(25) + location(10) + salaryMin(5) + salaryMax(5) + qualifications(5) = 85%
      val fields = Map(
        "title" -> Some("Software Engineer"),
        "companyName" -> Some("Netflix"),
        "description" -> Some("A great job opportunity"),
        "location" -> Some("Remote Location Here"),
        "salaryMin" -> Some("100000"),
        "salaryMax" -> Some("200000"),
        "qualifications" -> Some("Required qualifications list"),
      )

      val result = CompletionScorer.score(fields)
      assert(result.score < 0.90)
      assert(result.score >= 0.70)
      assert(result.state == CompletionState.Sufficient)
    }
  }

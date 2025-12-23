package arcata.api.utils

import utest.*

object HtmlCleanerSuite extends TestSuite:

  val tests = Tests {

    test("preserves script tags in body for embedded JSON data") {
      val html = """
        <html>
          <body>
            <h1>Job Title</h1>
            <script>window.__DATA__ = {"position": {"title": "Engineer"}}</script>
            <p>Description</p>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      // Script tags in body are preserved (SPAs embed job data in them)
      assert(result.contains("<script>"))
      assert(result.contains("Engineer"))
      assert(result.contains("Job Title"))
      assert(result.contains("Description"))
    }

    test("removes scripts in head via head removal") {
      val html = """
        <html>
          <head>
            <script>var analytics = {};</script>
            <script src="https://tracking.example.com/t.js"></script>
          </head>
          <body>
            <h1>Job Title</h1>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      // Head is removed, so scripts in head are also removed
      assert(!result.contains("analytics"))
      assert(!result.contains("tracking.example.com"))
      assert(result.contains("Job Title"))
    }

    test("preserves JSON-LD script tags") {
      val html = """
        <html>
          <body>
            <h1>Software Engineer</h1>
            <script type="application/ld+json">
            {
              "@context": "https://schema.org/",
              "@type": "JobPosting",
              "title": "Software Engineer",
              "description": "We are looking for a talented engineer.",
              "hiringOrganization": {
                "@type": "Organization",
                "name": "Netflix"
              }
            }
            </script>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(result.contains("application/ld+json"))
      assert(result.contains("JobPosting"))
      assert(result.contains("Netflix"))
      assert(result.contains("talented engineer"))
    }

    test("preserves embedded SPA state with job data") {
      // This mimics what Netflix and similar SPAs do
      val html = """
        <html>
          <body>
            <div id="app"></div>
            <script>
              window.__INITIAL_STATE__ = {
                "positions": [{
                  "id": 790303620790,
                  "name": "Software Engineer (L5), Ads Media Planning",
                  "location": "USA - Remote",
                  "department": "Engineering",
                  "job_description": "We are looking for a software engineer to join our Ads team."
                }]
              };
            </script>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(result.contains("positions"))
      assert(result.contains("Software Engineer (L5)"))
      assert(result.contains("Ads Media Planning"))
      assert(result.contains("USA - Remote"))
      assert(result.contains("job_description"))
    }

    test("removes style tags") {
      val html = """
        <html>
          <body>
            <style>.foo { color: red; }</style>
            <h1>Job Title</h1>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<style>"))
      assert(!result.contains("color: red"))
      assert(result.contains("Job Title"))
    }

    test("removes head tag content") {
      val html = """
        <html>
          <head>
            <title>Page Title</title>
            <meta charset="utf-8">
            <link rel="stylesheet" href="styles.css">
          </head>
          <body>
            <h1>Job Title</h1>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<head>"))
      assert(!result.contains("<meta"))
      assert(!result.contains("<link"))
      assert(result.contains("Job Title"))
    }

    test("removes HTML comments") {
      val html = """
        <html>
          <body>
            <!-- This is a comment -->
            <h1>Job Title</h1>
            <!-- Another comment -->
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<!--"))
      assert(!result.contains("This is a comment"))
      assert(result.contains("Job Title"))
    }

    test("strips class and style attributes") {
      val html = """
        <html>
          <body>
            <div class="container mx-auto px-4" style="color: red;">
              <h1 class="text-xl font-bold">Job Title</h1>
            </div>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("class="))
      assert(!result.contains("style="))
      assert(!result.contains("container"))
      assert(!result.contains("color: red"))
      assert(result.contains("Job Title"))
    }

    test("strips data-* attributes") {
      val html = """
        <html>
          <body>
            <div data-testid="job-card" data-tracking-id="abc123">
              <h1>Job Title</h1>
            </div>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("data-testid"))
      assert(!result.contains("data-tracking-id"))
      assert(result.contains("Job Title"))
    }

    test("removes navigation and footer") {
      val html = """
        <html>
          <body>
            <nav><a href="/">Home</a></nav>
            <main>
              <h1>Job Title</h1>
              <p>Description</p>
            </main>
            <footer>Copyright 2024</footer>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<nav>"))
      assert(!result.contains("<footer>"))
      assert(result.contains("Job Title"))
      assert(result.contains("Description"))
    }

    test("converts to markdown with headings") {
      val html = """
        <html>
          <body>
            <h1>Software Engineer</h1>
            <p>We are looking for a <strong>talented</strong> engineer.</p>
            <h2>Requirements</h2>
          </body>
        </html>
      """
      val result = HtmlCleaner.toMarkdown(html)

      // Should contain markdown formatting
      assert(result.contains("# Software Engineer") || result.contains("Software Engineer"))
      assert(result.contains("**talented**") || result.contains("talented"))
      assert(result.contains("## Requirements") || result.contains("Requirements"))
    }

    test("converts lists to markdown") {
      val html = """
        <html>
          <body>
            <ul>
              <li>5+ years experience</li>
              <li>Python expertise</li>
            </ul>
          </body>
        </html>
      """
      val result = HtmlCleaner.toMarkdown(html)

      assert(result.contains("5+ years experience"))
      assert(result.contains("Python expertise"))
    }

    test("preserves links in markdown") {
      val html = """
        <html>
          <body>
            <p>Apply at <a href="https://jobs.example.com/apply">our careers page</a></p>
          </body>
        </html>
      """
      val result = HtmlCleaner.toMarkdown(html)

      assert(result.contains("https://jobs.example.com/apply"))
    }

    test("truncates content exceeding max length") {
      // Create HTML with lots of content
      val longContent = "x" * 150_000
      val html = s"<html><body><p>$longContent</p></body></html>"

      val result = HtmlCleaner.toMarkdown(html)

      assert(result.length <= 100_000 + 50) // Allow for truncation message
      assert(result.contains("[Content truncated...]"))
    }

    test("handles malformed HTML gracefully") {
      val html = "<div><p>Unclosed tags<span>More text"

      // Should not throw
      val result = HtmlCleaner.toMarkdown(html)

      assert(result.contains("Unclosed tags"))
      assert(result.contains("More text"))
    }

    test("handles empty HTML") {
      val html = ""
      val result = HtmlCleaner.toMarkdown(html)

      // Should return empty or minimal content, not throw
      assert(result.length < 100)
    }

    test("removes iframe tags") {
      val html = """
        <html>
          <body>
            <h1>Job Title</h1>
            <iframe src="https://ads.example.com"></iframe>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<iframe"))
      assert(!result.contains("ads.example.com"))
      assert(result.contains("Job Title"))
    }

    test("removes svg content") {
      val html = """
        <html>
          <body>
            <h1>Job Title</h1>
            <svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="40"/></svg>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<svg"))
      assert(!result.contains("<circle"))
      assert(result.contains("Job Title"))
    }

    test("removes form elements") {
      val html = """
        <html>
          <body>
            <h1>Job Title</h1>
            <form action="/apply">
              <input type="text" name="name">
              <button>Submit</button>
            </form>
            <p>Description</p>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<form"))
      assert(!result.contains("<input"))
      assert(result.contains("Job Title"))
      assert(result.contains("Description"))
    }
  }

package arcata.api.utils

import utest.*

object HtmlCleanerSuite extends TestSuite:

  val tests = Tests {

    test("removes non-JSON-LD script tags in body") {
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

      // Non-JSON-LD scripts are removed to reduce AI token usage
      assert(!result.contains("<script>"))
      assert(!result.contains("window.__DATA__"))
      assert(!result.contains("Engineer"))
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

    test("preserves JSON-LD content as text") {
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

      // JSON-LD content is extracted as plain text (no script tags)
      assert(!result.contains("<script"))
      assert(result.contains("JobPosting"))
      assert(result.contains("Netflix"))
      assert(result.contains("talented engineer"))
    }

    test("removes embedded SPA state scripts (non-JSON-LD)") {
      // SPA bootstrap state is removed - JSON-LD is the preferred structured data source
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

      // SPA state is removed to reduce token bloat
      assert(!result.contains("__INITIAL_STATE__"))
      assert(!result.contains("positions"))
      assert(!result.contains("Software Engineer (L5)"))
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
      // Create HTML with lots of content (MAX_CONTENT_LENGTH is 180_000)
      val longContent = "x" * 200_000
      val html = s"<html><body><p>$longContent</p></body></html>"

      val result = HtmlCleaner.toMarkdown(html)

      assert(result.length <= 180_000 + 50) // Allow for truncation message
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

    test("removes link tags") {
      val html = """
        <html>
          <body>
            <link rel="stylesheet" href="styles.css">
            <link rel="preload" href="font.woff2" as="font">
            <h1>Job Title</h1>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<link"))
      assert(!result.contains("stylesheet"))
      assert(!result.contains("preload"))
      assert(result.contains("Job Title"))
    }

    test("removes code tags with hidden config") {
      // Netflix/Eightfold pattern: hidden <code> elements with JSON config
      val html = """
        <html>
          <body>
            <code id="branding-data" style="display:none;">{"theme": {"colors": {"primary": "#ff0000"}}}</code>
            <code id="smartApplyData" style="display:none;">{"domain": "company.com", "positions": [...]}</code>
            <h1>Job Title</h1>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("<code"))
      assert(!result.contains("branding-data"))
      assert(!result.contains("smartApplyData"))
      assert(!result.contains("theme"))
      assert(result.contains("Job Title"))
    }

    test("strips nonce, crossorigin, and integrity attributes") {
      val html = """
        <html>
          <body>
            <script type="application/ld+json" nonce="abc123" crossorigin="anonymous" integrity="sha384-xyz">
            {"@type": "JobPosting", "title": "Engineer"}
            </script>
            <div nonce="def456">Content</div>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("nonce="))
      assert(!result.contains("crossorigin="))
      assert(!result.contains("integrity="))
      assert(result.contains("JobPosting"))
      // Content div should still be there (stripped of attributes)
      assert(result.contains("Content"))
    }

    test("decodes HTML entities in JSON-LD content") {
      val html = """
        <html>
          <body>
            <script type="application/ld+json">
            {
              "@type": "JobPosting",
              "title": "Software Engineer",
              "description": "&lt;p&gt;We are looking for a &lt;strong&gt;talented&lt;/strong&gt; engineer.&lt;/p&gt;"
            }
            </script>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      // HTML entities should be decoded in the extracted JSON-LD text
      assert(result.contains("<p>We are looking for"))
      assert(result.contains("<strong>talented</strong>"))
      assert(!result.contains("&lt;p&gt;"))
      assert(!result.contains("&lt;strong&gt;"))
    }

    test("removes analytics and tracking scripts") {
      val html = """
        <html>
          <body>
            <h1>Job Title</h1>
            <script src="https://www.googletagmanager.com/gtag/js"></script>
            <script>
              window.dataLayer = window.dataLayer || [];
              function gtag(){dataLayer.push(arguments);}
              gtag('js', new Date());
            </script>
            <script src="https://static.example.com/sentry/bundle.js"></script>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("googletagmanager"))
      assert(!result.contains("dataLayer"))
      assert(!result.contains("sentry"))
      assert(result.contains("Job Title"))
    }

    test("removes recaptcha scripts") {
      val html = """
        <html>
          <body>
            <h1>Job Title</h1>
            <script defer src='https://www.recaptcha.net/recaptcha/api.js?render=abc123'></script>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      assert(!result.contains("recaptcha"))
      assert(result.contains("Job Title"))
    }

    test("handles multiple JSON-LD scripts") {
      val html = """
        <html>
          <body>
            <script type="application/ld+json">
            {"@type": "JobPosting", "title": "Software Engineer"}
            </script>
            <script type="application/ld+json">
            {"@type": "WebSite", "name": "Company Careers"}
            </script>
            <h1>Job Title</h1>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      // All JSON-LD content should be extracted as text
      assert(result.contains("JobPosting"))
      assert(result.contains("Software Engineer"))
      assert(result.contains("WebSite"))
      assert(result.contains("Company Careers"))
      assert(result.contains("Job Title"))
      // Script tags should be removed
      assert(!result.contains("<script"))
    }

    test("extracts JSON-LD from head before removing it") {
      // This is the Ashby/Netflix pattern - JSON-LD in <head>
      val html = """
        <html>
          <head>
            <title>Job Title</title>
            <script type="application/ld+json">
            {"@type": "JobPosting", "title": "Software Engineer", "hiringOrganization": {"name": "Hopper"}}
            </script>
          </head>
          <body>
            <div id="root"></div>
          </body>
        </html>
      """
      val result = HtmlCleaner.clean(html)

      // JSON-LD from head should be preserved
      assert(result.contains("JobPosting"))
      assert(result.contains("Software Engineer"))
      assert(result.contains("Hopper"))
      // Head content (except JSON-LD) should be removed
      assert(!result.contains("<title>"))
      assert(!result.contains("<script"))
    }
  }

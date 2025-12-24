package arcata.api.extraction

import utest.*

object JsonPathTraverserSuite extends TestSuite:

  // Sample JSON matching Schema.org JobPosting structure
  val jobPostingJson: String = """
    {
      "@type": "JobPosting",
      "title": "Software Engineer",
      "description": "We are looking for a talented engineer.",
      "hiringOrganization": {
        "name": "Netflix",
        "@type": "Organization"
      },
      "baseSalary": {
        "currency": "USD",
        "value": {
          "minValue": 100000,
          "maxValue": 720000
        }
      },
      "jobLocation": [
        {
          "address": {
            "addressLocality": "Los Angeles",
            "addressRegion": "CA",
            "addressCountry": "US"
          }
        },
        {
          "address": {
            "addressLocality": "New York",
            "addressRegion": "NY"
          }
        }
      ],
      "employmentType": "FULL_TIME",
      "datePosted": "2024-01-15",
      "validThrough": "2024-06-15",
      "isRemote": true,
      "skills": ["Java", "Scala", "Python"]
    }
  """

  val json: ujson.Value = ujson.read(jobPostingJson)

  val tests = Tests {

    test("simple property access") {
      assert(JsonPathTraverser.getString(json, "$.title") == Some("Software Engineer"))
      assert(JsonPathTraverser.getString(json, "$.@type") == Some("JobPosting"))
      assert(JsonPathTraverser.getString(json, "$.employmentType") == Some("FULL_TIME"))
    }

    test("nested property access") {
      assert(JsonPathTraverser.getString(json, "$.hiringOrganization.name") == Some("Netflix"))
      assert(JsonPathTraverser.getString(json, "$.baseSalary.currency") == Some("USD"))
    }

    test("deep nested property access") {
      assert(JsonPathTraverser.getNumber(json, "$.baseSalary.value.minValue") == Some(100000.0))
      assert(JsonPathTraverser.getNumber(json, "$.baseSalary.value.maxValue") == Some(720000.0))
    }

    test("array index access") {
      val firstLocation = JsonPathTraverser.getString(json, "$.jobLocation[0].address.addressLocality")
      assert(firstLocation == Some("Los Angeles"))

      val secondLocation = JsonPathTraverser.getString(json, "$.jobLocation[1].address.addressLocality")
      assert(secondLocation == Some("New York"))
    }

    test("array index with nested path") {
      assert(JsonPathTraverser.getString(json, "$.jobLocation[0].address.addressRegion") == Some("CA"))
      assert(JsonPathTraverser.getString(json, "$.jobLocation[1].address.addressRegion") == Some("NY"))
    }

    test("get array") {
      val skills = JsonPathTraverser.getArray(json, "$.skills")
      assert(skills.isDefined)
      assert(skills.get.length == 3)
      assert(skills.get.map(_.str) == Seq("Java", "Scala", "Python"))
    }

    test("get boolean") {
      assert(JsonPathTraverser.getBool(json, "$.isRemote") == Some(true))
    }

    test("get integer") {
      assert(JsonPathTraverser.getInt(json, "$.baseSalary.value.minValue") == Some(100000))
    }

    test("returns None for missing property") {
      assert(JsonPathTraverser.getString(json, "$.nonexistent") == None)
      assert(JsonPathTraverser.getString(json, "$.hiringOrganization.nonexistent") == None)
    }

    test("returns None for missing nested path") {
      assert(JsonPathTraverser.getString(json, "$.foo.bar.baz") == None)
    }

    test("returns None for out of bounds array index") {
      assert(JsonPathTraverser.getString(json, "$.jobLocation[99].address.city") == None)
      assert(JsonPathTraverser.getString(json, "$.skills[10]") == None)
    }

    test("returns None for type mismatch") {
      // title is a string, not a number
      assert(JsonPathTraverser.getNumber(json, "$.title") == None)
      // isRemote is boolean, not string (though ujson might coerce)
      assert(JsonPathTraverser.getInt(json, "$.isRemote") == None)
    }

    test("path without $ prefix works") {
      assert(JsonPathTraverser.getString(json, "title") == Some("Software Engineer"))
      assert(JsonPathTraverser.getString(json, "hiringOrganization.name") == Some("Netflix"))
    }

    test("empty path returns root") {
      val root = JsonPathTraverser.get(json, "$")
      assert(root.isDefined)
      assert(root.get.obj.contains("title"))
    }

    test("get raw value") {
      val org = JsonPathTraverser.get(json, "$.hiringOrganization")
      assert(org.isDefined)
      assert(org.get.obj("name").str == "Netflix")
    }

    test("complex real-world path") {
      // This is the actual path we'd use for Netflix salary extraction
      val minSalary = JsonPathTraverser.getNumber(json, "$.baseSalary.value.minValue")
      val maxSalary = JsonPathTraverser.getNumber(json, "$.baseSalary.value.maxValue")
      val currency = JsonPathTraverser.getString(json, "$.baseSalary.currency")

      assert(minSalary == Some(100000.0))
      assert(maxSalary == Some(720000.0))
      assert(currency == Some("USD"))
    }

    test("handles array at root level") {
      val arrayJson = ujson.read("""[{"name": "first"}, {"name": "second"}]""")
      assert(JsonPathTraverser.getString(arrayJson, "$[0].name") == Some("first"))
      assert(JsonPathTraverser.getString(arrayJson, "$[1].name") == Some("second"))
    }

    test("consecutive array indices") {
      val nestedArray = ujson.read("""{"data": [[1, 2], [3, 4]]}""")
      assert(JsonPathTraverser.getNumber(nestedArray, "$.data[0][0]") == Some(1.0))
      assert(JsonPathTraverser.getNumber(nestedArray, "$.data[0][1]") == Some(2.0))
      assert(JsonPathTraverser.getNumber(nestedArray, "$.data[1][0]") == Some(3.0))
    }
  }

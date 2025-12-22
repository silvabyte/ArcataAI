export default async function fetchAndParseJobListing(url: string) {
  //TODO: refactor this to live in a backend service and be far more robust, using ai, , etc to parse the html and extract the data
  try {
    const response = await fetch(url);
    const html = await response.text();
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, "text/html");

    const jobObject = {
      application_email: null, // Not available in HTML
      application_url:
        doc.querySelector(".btn.btn-primary")?.getAttribute("href") || null,
      benefits:
        Array.from(doc.querySelectorAll("ul > li > p")).map((el) =>
          el.textContent?.trim()
        ) || null,
      category:
        doc
          .querySelector(".Layout--job .container > div > div > h1 + div + div")
          ?.textContent?.trim() ?? null,
      closing_date: null, // Not available in HTML
      company_id: null, // Not available in HTML
      contact_email: null, // Not available in HTML
      contact_phone: null, // Not available in HTML
      description:
        doc.querySelector(".Layout--job p")?.textContent?.trim() || null,
      education_level: null, // Not available in HTML
      experience_level: null, // Not available in HTML
      job_id: null, // Not available in HTML
      job_type: null, // Not available in HTML
      location:
        doc
          .querySelector(".Layout--job .container > div > div > div.my-2")
          ?.textContent?.trim() || null,
      posted_date: null, // Not available in HTML
      preferred_qualifications: null, // Not available in HTML
      qualifications: null, // Not available in HTML
      responsibilities: null, // Not available in HTML
      salary_range: null, // Not available in HTML
      status: null, // Not available in HTML
      title:
        doc.querySelector(".Layout--job h1")?.textContent?.trim() ||
        "Unknown Title",
    };

    return jobObject;
  } catch (error) {
    console.error("Error fetching or parsing:", error);
    return null;
  }
}

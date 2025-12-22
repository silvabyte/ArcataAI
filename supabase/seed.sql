-- Seed file for local development
-- Run via: supabase db reset (runs migrations then seed.sql)
-- Or manually: psql -f supabase/seed.sql
--
-- Note: We only seed companies and jobs. 
-- job_stream entries require a profile (which requires auth.users).
-- The app falls back to showing all jobs when job_stream is empty.

-- ============================================================================
-- COMPANIES (10 companies)
-- ============================================================================

INSERT INTO companies (company_id, company_name, company_domain, company_city, company_state, employee_count_min, employee_count_max, primary_industry)
VALUES
  (1, 'Acme Corp', 'acme.com', 'San Francisco', 'CA', 500, 1000, 'Technology'),
  (2, 'TechStart Inc', 'techstart.io', 'Austin', 'TX', 50, 100, 'Software'),
  (3, 'DataFlow Systems', 'dataflow.dev', 'Seattle', 'WA', 200, 500, 'Data Analytics'),
  (4, 'CloudNine Solutions', 'cloudnine.co', 'Denver', 'CO', 100, 200, 'Cloud Computing'),
  (5, 'GreenLeaf Technologies', 'greenleaf.tech', 'Portland', 'OR', 25, 50, 'Sustainability Tech'),
  (6, 'Quantum Innovations', 'quantum-innov.com', 'Boston', 'MA', 75, 150, 'Research & Development'),
  (7, 'Urban Dynamics', 'urbandynamics.io', 'New York', 'NY', 300, 600, 'Real Estate Tech'),
  (8, 'HealthBridge AI', 'healthbridge.ai', 'Chicago', 'IL', 150, 300, 'Healthcare Technology'),
  (9, 'FinEdge Partners', 'finedge.com', 'Miami', 'FL', 80, 160, 'Financial Services'),
  (10, 'Nexus Labs', 'nexuslabs.dev', 'Remote', NULL, 20, 40, 'Developer Tools');

-- Reset sequence to avoid conflicts with future inserts
SELECT setval('companies_company_id_seq', 10, true);

-- ============================================================================
-- JOBS (20 jobs across various companies)
-- ============================================================================

INSERT INTO jobs (
  job_id, company_id, title, description, location, is_remote, job_type,
  experience_level, education_level, salary_min, salary_max, salary_currency,
  qualifications, responsibilities, application_url, posted_date
) VALUES

-- Acme Corp (company_id: 1)
(1, 1, 'Senior Software Engineer', 
  'Join our platform team to build scalable microservices. You''ll work on high-throughput systems processing millions of requests daily.',
  'San Francisco, CA', false, 'full-time', 'senior', 'bachelors',
  150000, 200000, 'USD',
  ARRAY['5+ years experience with distributed systems', 'Strong knowledge of Go or Rust', 'Experience with Kubernetes'],
  ARRAY['Design and implement microservices', 'Mentor junior engineers', 'Participate in architecture decisions'],
  'https://careers.acme.com/senior-swe', NOW() - INTERVAL '3 days'),

(2, 1, 'Product Designer',
  'Shape the future of our enterprise products. Work closely with engineering and product to create intuitive experiences.',
  'San Francisco, CA', true, 'full-time', 'mid', 'bachelors',
  120000, 160000, 'USD',
  ARRAY['3+ years product design experience', 'Figma expertise', 'Experience with design systems'],
  ARRAY['Lead design for new features', 'Conduct user research', 'Maintain design system'],
  'https://careers.acme.com/product-designer', NOW() - INTERVAL '7 days'),

-- TechStart Inc (company_id: 2)
(3, 2, 'Full Stack Developer',
  'Build features end-to-end in our fast-paced startup environment. We use React, Node.js, and PostgreSQL.',
  'Austin, TX', true, 'full-time', 'mid', 'bachelors',
  100000, 140000, 'USD',
  ARRAY['3+ years full stack experience', 'React and Node.js proficiency', 'PostgreSQL experience'],
  ARRAY['Develop new product features', 'Write clean, tested code', 'Collaborate with product team'],
  'https://techstart.io/careers/fullstack', NOW() - INTERVAL '2 days'),

(4, 2, 'Engineering Intern',
  'Summer internship program for aspiring software engineers. Work on real features with mentorship from senior engineers.',
  'Austin, TX', false, 'internship', 'intern', 'none',
  25, 35, 'USD',  -- hourly rate stored as-is
  ARRAY['Currently pursuing CS degree', 'Basic programming knowledge', 'Eagerness to learn'],
  ARRAY['Contribute to codebase', 'Participate in code reviews', 'Present project at end of internship'],
  'https://techstart.io/careers/intern', NOW() - INTERVAL '14 days'),

-- DataFlow Systems (company_id: 3)
(5, 3, 'Data Engineer',
  'Build and maintain our data pipeline infrastructure. Work with petabytes of data using modern tooling.',
  'Seattle, WA', true, 'full-time', 'senior', 'masters',
  160000, 210000, 'USD',
  ARRAY['5+ years data engineering experience', 'Expert in Spark and Airflow', 'Strong SQL skills'],
  ARRAY['Design data pipelines', 'Optimize query performance', 'Ensure data quality'],
  'https://dataflow.dev/jobs/data-engineer', NOW() - INTERVAL '5 days'),

(6, 3, 'ML Engineer',
  'Deploy and scale machine learning models in production. Partner with data scientists to productionize research.',
  'Seattle, WA', false, 'full-time', 'senior', 'masters',
  170000, 220000, 'USD',
  ARRAY['4+ years ML engineering', 'Python and PyTorch/TensorFlow', 'MLOps experience'],
  ARRAY['Deploy ML models at scale', 'Build feature stores', 'Monitor model performance'],
  'https://dataflow.dev/jobs/ml-engineer', NOW() - INTERVAL '1 day'),

-- CloudNine Solutions (company_id: 4)
(7, 4, 'DevOps Engineer',
  'Manage cloud infrastructure across AWS and GCP. Automate everything and drive our reliability initiatives.',
  'Denver, CO', true, 'full-time', 'mid', 'bachelors',
  130000, 170000, 'USD',
  ARRAY['3+ years DevOps experience', 'AWS or GCP certification', 'Terraform expertise'],
  ARRAY['Maintain cloud infrastructure', 'Implement CI/CD pipelines', 'On-call rotation'],
  'https://cloudnine.co/careers/devops', NOW() - INTERVAL '10 days'),

(8, 4, 'Site Reliability Engineer',
  'Ensure 99.99% uptime for our platform. Debug complex distributed systems issues.',
  'Remote', true, 'full-time', 'advanced', 'bachelors',
  175000, 225000, 'USD',
  ARRAY['6+ years SRE or systems experience', 'Strong Linux fundamentals', 'Incident management experience'],
  ARRAY['Drive reliability improvements', 'Lead incident response', 'Capacity planning'],
  'https://cloudnine.co/careers/sre', NOW() - INTERVAL '4 days'),

-- GreenLeaf Technologies (company_id: 5)
(9, 5, 'Frontend Developer',
  'Build beautiful, accessible web applications for sustainability-focused products.',
  'Portland, OR', true, 'full-time', 'early', 'bachelors',
  85000, 115000, 'USD',
  ARRAY['1-2 years frontend experience', 'React or Vue.js', 'CSS/Tailwind proficiency'],
  ARRAY['Implement UI components', 'Ensure accessibility compliance', 'Write unit tests'],
  'https://greenleaf.tech/jobs/frontend', NOW() - INTERVAL '6 days'),

(10, 5, 'Part-Time Technical Writer',
  'Create documentation and guides for our developer-focused products.',
  'Remote', true, 'part-time', 'mid', 'bachelors',
  50000, 70000, 'USD',  -- pro-rated annual
  ARRAY['2+ years technical writing', 'Understanding of APIs', 'Excellent written communication'],
  ARRAY['Write API documentation', 'Create tutorials', 'Maintain knowledge base'],
  'https://greenleaf.tech/jobs/tech-writer', NOW() - INTERVAL '12 days'),

-- Quantum Innovations (company_id: 6)
(11, 6, 'Research Scientist',
  'Conduct fundamental research in quantum computing algorithms.',
  'Boston, MA', false, 'full-time', 'advanced', 'masters',
  180000, 240000, 'USD',
  ARRAY['PhD preferred in Physics/CS', 'Published research', 'Quantum computing expertise'],
  ARRAY['Conduct original research', 'Publish papers', 'Collaborate with academic partners'],
  'https://quantum-innov.com/careers/research', NOW() - INTERVAL '20 days'),

-- Urban Dynamics (company_id: 7)
(12, 7, 'Backend Engineer',
  'Build APIs and services for our real estate analytics platform.',
  'New York, NY', false, 'full-time', 'mid', 'bachelors',
  140000, 180000, 'USD',
  ARRAY['3+ years backend experience', 'Python or Go', 'REST API design'],
  ARRAY['Design and implement APIs', 'Optimize database queries', 'Write technical specs'],
  'https://urbandynamics.io/jobs/backend', NOW() - INTERVAL '8 days'),

(13, 7, 'Engineering Manager',
  'Lead a team of 6-8 engineers building our core platform.',
  'New York, NY', false, 'full-time', 'director', 'bachelors',
  200000, 260000, 'USD',
  ARRAY['5+ years engineering experience', '2+ years management', 'Track record of delivery'],
  ARRAY['Manage and grow team', 'Drive technical roadmap', 'Partner with product'],
  'https://urbandynamics.io/jobs/em', NOW() - INTERVAL '15 days'),

-- HealthBridge AI (company_id: 8)
(14, 8, 'Healthcare Data Analyst',
  'Analyze healthcare data to improve patient outcomes. HIPAA compliance required.',
  'Chicago, IL', false, 'full-time', 'mid', 'masters',
  95000, 130000, 'USD',
  ARRAY['3+ years healthcare analytics', 'SQL and Python', 'HIPAA knowledge'],
  ARRAY['Analyze patient data', 'Create dashboards', 'Present findings to stakeholders'],
  'https://healthbridge.ai/careers/analyst', NOW() - INTERVAL '9 days'),

(15, 8, 'Senior iOS Developer',
  'Build our patient-facing mobile application. Focus on privacy and security.',
  'Chicago, IL', true, 'full-time', 'senior', 'bachelors',
  155000, 195000, 'USD',
  ARRAY['5+ years iOS development', 'Swift expertise', 'Healthcare app experience preferred'],
  ARRAY['Develop iOS features', 'Code review', 'Security best practices'],
  'https://healthbridge.ai/careers/ios', NOW() - INTERVAL '3 days'),

-- FinEdge Partners (company_id: 9)
(16, 9, 'Contract Backend Developer',
  '6-month contract to build new trading APIs. Potential for conversion.',
  'Miami, FL', true, 'contract', 'senior', 'bachelors',
  100, 150, 'USD',  -- hourly
  ARRAY['5+ years backend experience', 'Financial services experience', 'Low-latency systems'],
  ARRAY['Build trading APIs', 'Ensure regulatory compliance', 'Performance optimization'],
  'https://finedge.com/jobs/contract-backend', NOW() - INTERVAL '2 days'),

(17, 9, 'Junior QA Engineer',
  'Join our QA team to ensure software quality across our fintech products.',
  'Miami, FL', false, 'full-time', 'early', 'bachelors',
  70000, 90000, 'USD',
  ARRAY['1+ years QA experience', 'Test automation basics', 'Attention to detail'],
  ARRAY['Write test cases', 'Perform manual testing', 'Learn automation frameworks'],
  'https://finedge.com/jobs/qa', NOW() - INTERVAL '11 days'),

-- Nexus Labs (company_id: 10) - Fully Remote Company
(18, 10, 'Open Source Developer',
  'Contribute to and maintain our popular open source developer tools.',
  'Remote', true, 'full-time', 'mid', 'none',
  110000, 150000, 'USD',
  ARRAY['3+ years software development', 'Open source contributions', 'Strong communication skills'],
  ARRAY['Maintain OSS projects', 'Review community PRs', 'Write documentation'],
  'https://nexuslabs.dev/careers/oss', NOW() - INTERVAL '1 day'),

(19, 10, 'Developer Advocate',
  'Be the voice of developers. Create content, speak at conferences, gather feedback.',
  'Remote', true, 'full-time', 'mid', 'bachelors',
  120000, 160000, 'USD',
  ARRAY['3+ years development experience', 'Public speaking ability', 'Content creation skills'],
  ARRAY['Create tutorials and demos', 'Speak at conferences', 'Gather developer feedback'],
  'https://nexuslabs.dev/careers/devrel', NOW() - INTERVAL '5 days'),

(20, 10, 'Part-Time Community Manager',
  'Manage our Discord and GitHub communities. Engage with users and triage issues.',
  'Remote', true, 'part-time', 'early', 'none',
  40000, 55000, 'USD',  -- pro-rated annual
  ARRAY['1+ years community management', 'Technical background helpful', 'Excellent communication'],
  ARRAY['Moderate community channels', 'Triage GitHub issues', 'Organize community events'],
  'https://nexuslabs.dev/careers/community', NOW() - INTERVAL '7 days');

-- Reset sequence
SELECT setval('jobs_job_id_seq', 20, true);

-- ============================================================================
-- NOTE: job_stream entries not seeded
-- ============================================================================
-- job_stream requires a profile_id that references auth.users.
-- For local dev without auth, the app falls back to showing all jobs directly.
-- When testing with auth, job_stream entries will be created dynamically.

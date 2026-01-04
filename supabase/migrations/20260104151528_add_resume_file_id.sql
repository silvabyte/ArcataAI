-- Add resume_file_id column to job_profiles table
-- Stores the ObjectStorage ID of the uploaded resume file

ALTER TABLE job_profiles 
ADD COLUMN resume_file_id TEXT;

COMMENT ON COLUMN job_profiles.resume_file_id IS 'ObjectStorage ID of the uploaded resume file used to create this profile';

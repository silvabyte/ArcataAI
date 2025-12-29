-- Migration: Create conversations table
-- Description: Multi-channel communication tracking (email, meetings, phone, chat)
-- Dependencies: 20241229000002_profiles.sql, 20241229000007_job_applications.sql

-- ============================================================================
-- TABLE: conversations
-- ============================================================================
-- Tracks all communication interactions across multiple channels.
-- Links to specific job applications and user profiles.

CREATE TABLE conversations (
  conversation_id SERIAL PRIMARY KEY,
  profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  application_id INTEGER NOT NULL REFERENCES job_applications(application_id) ON DELETE CASCADE,
  channel TEXT NOT NULL,
  subject TEXT,
  summary TEXT,
  occurred_at TIMESTAMPTZ NOT NULL,
  channel_data JSONB,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- TRIGGER: Auto-update updated_at on conversations
-- ============================================================================

CREATE TRIGGER update_conversations_updated_at
  BEFORE UPDATE ON conversations
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own conversations
CREATE POLICY conversations_select_own
  ON conversations
  FOR SELECT
  TO authenticated
  USING (auth.uid() = profile_id);

-- Policy: Users can insert their own conversations
CREATE POLICY conversations_insert_own
  ON conversations
  FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can update their own conversations
CREATE POLICY conversations_update_own
  ON conversations
  FOR UPDATE
  TO authenticated
  USING (auth.uid() = profile_id)
  WITH CHECK (auth.uid() = profile_id);

-- Policy: Users can delete their own conversations
CREATE POLICY conversations_delete_own
  ON conversations
  FOR DELETE
  TO authenticated
  USING (auth.uid() = profile_id);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX conversations_profile_id_idx ON conversations(profile_id);
CREATE INDEX conversations_application_id_idx ON conversations(application_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE conversations IS 'Multi-channel communication tracking for job applications';
COMMENT ON COLUMN conversations.conversation_id IS 'Primary key, auto-incremented';
COMMENT ON COLUMN conversations.profile_id IS 'Foreign key to profiles.id (CASCADE delete)';
COMMENT ON COLUMN conversations.application_id IS 'Foreign key to job_applications.application_id (CASCADE delete)';
COMMENT ON COLUMN conversations.channel IS 'Communication channel type (email, meeting, phone, chat)';
COMMENT ON COLUMN conversations.subject IS 'Subject line or title of the conversation';
COMMENT ON COLUMN conversations.summary IS 'Brief summary of the conversation content';
COMMENT ON COLUMN conversations.occurred_at IS 'When the conversation took place';
COMMENT ON COLUMN conversations.channel_data IS 'Channel-specific data in versioned JSON envelope format';

-- ============================================================================
-- CHANNEL DATA ENVELOPE EXAMPLES
-- ============================================================================
-- Email: {"version": 1, "data": {"sender": "...", "receiver": "...", "message": "...", "attachment_links": [], "thread_id": "...", "forwarded_from": "..."}}
-- Meeting: {"version": 1, "data": {"provider": "audetic", "external_id": "...", "duration_seconds": 1800, "participants": [], "transcript_url": "...", "recording_url": "...", "notes": "..."}}
-- Phone: {"version": 1, "data": {"phone_number": "+1...", "direction": "inbound", "duration_seconds": 300, "notes": "..."}}
-- Chat: {"version": 1, "data": {"platform": "linkedin", "messages": [...]}}

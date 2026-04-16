-- since 1.203
-- SaaS Compatible
-- NEXUS-51201: Add context column to webhook table for Firewall/Lifecycle differentiation
-- This allows webhooks with shared event types (APPLICATION_EVALUATION, ORG_APP_MANAGEMENT) to be
-- properly scoped to either Firewall or Lifecycle context without auto-selecting additional events.

-- Add nullable context column (firewall, lifecycle, or NULL for pre-upgrade webhooks)
-- NULL values represent webhooks created before context separation was introduced.
-- These webhooks remain visible in the current product context (determined by license).
-- New webhooks are explicitly assigned 'firewall' or 'lifecycle' context.
ALTER TABLE webhook ADD COLUMN context varchar(20);

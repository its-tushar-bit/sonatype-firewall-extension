-- Since 1.200
-- SaaS Compatible

-- CLM-37867: H2 version - no-op for single-tenant IQ
-- This migration is specific to MTIQ (PostgreSQL). For H2 (single-tenant), do nothing.

-- No-op statement to satisfy script runner
SELECT 1;

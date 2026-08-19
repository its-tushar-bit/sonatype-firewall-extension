-- since 1.177
-- SaaS compatible

ALTER TABLE vulnerability_exploitability
    ADD COLUMN last_updated_by VARCHAR(255);

-- since 1.180
-- SaaS compatible

ALTER TABLE third_party_scan
    ADD COLUMN previous_scan_id VARCHAR(50);

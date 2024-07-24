-- since 1.180
-- SaaS compatible

ALTER TABLE third_party_scan
    ADD COLUMN filtered_scan_file VARCHAR(1000);

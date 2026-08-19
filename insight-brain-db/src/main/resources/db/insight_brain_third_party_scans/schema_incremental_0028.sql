-- since 1.176
-- SaaS compatible

ALTER TABLE file_coordinate
    ADD dependency_type VARCHAR(2);

ALTER TABLE file_coordinate
    ADD identification_sources VARCHAR(100);

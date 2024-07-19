-- since 1.180
-- SaaS Compatible
ALTER TABLE coordinate_security ADD COLUMN IF NOT EXISTS sbom_metadata_id VARCHAR(50);

CREATE INDEX IF NOT EXISTS sbom_metadata_third_party_file_id_idx ON sbom_metadata(third_party_file_id);

UPDATE coordinate_security cs
SET sbom_metadata_id = (
    SELECT sm.sbom_metadata_id
    FROM file_coordinate fc
             INNER JOIN sbom_metadata sm
                        ON fc.third_party_file_id = sm.third_party_file_id
    WHERE cs.file_coordinate_id = fc.file_coordinate_id
)
WHERE sbom_metadata_id is NULL
AND EXISTS (
    SELECT 1
    FROM file_coordinate fc
             INNER JOIN sbom_metadata sm
                        ON fc.third_party_file_id = sm.third_party_file_id
    WHERE cs.file_coordinate_id = fc.file_coordinate_id
);

-- since 1.188
-- SaaS compatible

CREATE INDEX file_coordinate_third_party_file_id_component_ref_idx ON file_coordinate (third_party_file_id, component_ref);
CREATE INDEX file_coordinate_third_party_file_id_hash_idx ON file_coordinate (third_party_file_id, hash);
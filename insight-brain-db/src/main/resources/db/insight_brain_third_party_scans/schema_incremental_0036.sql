-- since 1.183
-- SaaS compatible

CREATE TABLE third_party_unknown_component
(
    unknown_component_id VARCHAR(50)   NOT NULL,
    filename             VARCHAR(1000) NOT NULL,
    hash                 VARCHAR(20)   NOT NULL,
    dependency_type      VARCHAR(2),
    third_party_file_id  VARCHAR(50)   NOT NULL,
    CONSTRAINT unknown_component_pk PRIMARY KEY (unknown_component_id),
    CONSTRAINT unknown_component_uk UNIQUE (unknown_component_id),
    CONSTRAINT unknown_component_third_party_file_id_fk FOREIGN KEY (third_party_file_id)
        REFERENCES third_party_file (third_party_file_id)
);

CREATE INDEX unknown_component_third_party_file_id_idx
    ON third_party_unknown_component (third_party_file_id);


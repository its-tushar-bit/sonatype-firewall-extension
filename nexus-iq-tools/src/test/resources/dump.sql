CREATE SCHEMA insight_brain_ods;

CREATE TABLE insight_brain_ods.test_table (
    test_column timestamp (128) NOT NULL
);

INSERT INTO insight_brain_ods.test_table (test_column) VALUES ('2020-01-01');

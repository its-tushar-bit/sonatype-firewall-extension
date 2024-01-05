CREATE SCHEMA insight_brain_test;

CREATE TABLE insight_brain_test.test_table (
    test_column varchar(128) NOT NULL
);

INSERT INTO insight_brain_test.test_table (test_column) VALUES ('test-value');

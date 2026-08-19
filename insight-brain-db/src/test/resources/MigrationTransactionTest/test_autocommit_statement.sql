-- First statement: valid INSERT (should be committed if autoCommit=true)
INSERT INTO test_autocommit (id, value) VALUES (1, 'first');

-- Second statement: invalid SQL (will cause failure)
INVALID SQL STATEMENT HERE;

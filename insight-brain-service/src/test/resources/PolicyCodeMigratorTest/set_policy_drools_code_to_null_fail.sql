SET SCHEMA insight_brain_ods;

-- Must fail because drools_code is not nullable
UPDATE policy SET drools_code = null;
-- Since 1.125

-- Remove unique constraint on document title. The constraint wasn't named, so dropping and re-creating column
ALTER TABLE attribution_report_template ADD COLUMN temp varchar(250) NULL;
UPDATE attribution_report_template SET temp = document_title;
ALTER TABLE attribution_report_template DROP COLUMN document_title;
ALTER TABLE attribution_report_template ADD COLUMN document_title varchar(250) NULL;
UPDATE attribution_report_template SET document_title = temp;
ALTER TABLE attribution_report_template DROP COLUMN temp;

ALTER TABLE attribution_report_template ADD COLUMN template_name varchar(250) NULL;
UPDATE attribution_report_template SET template_name = document_title WHERE template_name is NULL;
ALTER TABLE attribution_report_template ALTER COLUMN template_name SET NOT NULL;
ALTER TABLE attribution_report_template ADD CONSTRAINT template_name_uk UNIQUE (template_name);

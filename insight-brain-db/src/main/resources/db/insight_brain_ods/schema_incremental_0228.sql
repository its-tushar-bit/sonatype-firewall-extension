-- Since 1.120
ALTER TABLE attribution_report_template ADD PRIMARY KEY (attribution_report_template_id);
ALTER TABLE attribution_report_template ADD UNIQUE (document_title);
ALTER TABLE attribution_report_template ALTER COLUMN include_table_of_contents SET DEFAULT TRUE;
ALTER TABLE attribution_report_template ALTER COLUMN include_standard_license_texts SET DEFAULT TRUE;
ALTER TABLE attribution_report_template ALTER COLUMN include_appendix SET DEFAULT TRUE;

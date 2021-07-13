-- Since 1.120
-- Legal Attribution Report creation editor object
CREATE TABLE attribution_report_template (
  attribution_report_template_id varchar(50) NOT NULL,
  document_title varchar(250) NOT NULL,
  document_header varchar(500),
  document_footer varchar(500),
  include_table_of_contents boolean,
  include_standard_license_texts boolean,
  include_appendix boolean,
  last_updated_at timestamp NOT NULL
);


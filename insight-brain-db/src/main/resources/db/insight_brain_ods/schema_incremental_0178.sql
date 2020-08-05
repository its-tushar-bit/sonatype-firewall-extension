-- Since 1.97
CREATE TABLE firewall_ignore_patterns (
  firewall_ignore_patterns_id varchar(50) NOT NULL,
  firewall_ignore_patterns_json text NOT NULL,
  CONSTRAINT firewall_ignore_patterns_pk PRIMARY KEY (firewall_ignore_patterns_id)
);

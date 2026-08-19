-- Since 1.97
ALTER TABLE firewall_ignore_patterns
  ALTER COLUMN firewall_ignore_patterns_json DROP NOT NULL;

INSERT INTO firewall_ignore_patterns(firewall_ignore_patterns_id) VALUES ('firewall-ignore-patterns');

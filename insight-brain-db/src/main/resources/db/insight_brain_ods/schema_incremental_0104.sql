-- Since 1.25.0
ALTER TABLE ldap_server ADD COLUMN priority int;

ALTER TABLE ldap_server ADD CONSTRAINT ldap_server_priority_uk UNIQUE KEY (priority);

UPDATE ldap_server SET priority = 1;

ALTER TABLE ldap_server ALTER COLUMN priority SET NOT NULL;

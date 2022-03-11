-- Since 1.135
UPDATE organization SET repository_connection_enabled = NULL;
UPDATE application SET repository_connection_enabled = NULL;

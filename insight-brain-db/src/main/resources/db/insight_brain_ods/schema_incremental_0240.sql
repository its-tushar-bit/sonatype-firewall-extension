-- Since 1.127
CREATE TABLE repository_client_configuration
(
    repository_client_configuration_id varchar(50) NOT NULL,
    connection_timeout                 smallint, -- in seconds
    socket_timeout                     smallint, -- in seconds
    CONSTRAINT repository_client_configuration_pk PRIMARY KEY (repository_client_configuration_id)
);

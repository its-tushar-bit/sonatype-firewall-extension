-- Since 1.127
CREATE TABLE repository_connection
(
    repository_connection_id varchar(50)   NOT NULL,
    owner_id                 varchar(50)   NOT NULL,
    base_url                 varchar(2048) NOT NULL,
    username                 varchar(255),
    password                 varchar(255),
    CONSTRAINT repository_connection_pk PRIMARY KEY (repository_connection_id),
    CONSTRAINT repository_connection_url_uk UNIQUE (owner_id, base_url)
);

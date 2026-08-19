-- Since 1.146
CREATE TABLE vulnerability_group (
  vulnerability_group_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  vulnerability_group_name varchar(60) NOT NULL,
  CONSTRAINT vulnerability_group_pk PRIMARY KEY (vulnerability_group_id),
  CONSTRAINT vulnerability_group_uk UNIQUE (owner_id, vulnerability_group_name)
);

CREATE TABLE vulnerability_group_vulnerability (
  vulnerability_group_vulnerability_id varchar(50) NOT NULL,
  vulnerability_group_id varchar(50) NOT NULL,
  vulnerability_refid varchar(100) NOT NULL,
  CONSTRAINT vulnerability_group_vulnerability_pk PRIMARY KEY (vulnerability_group_vulnerability_id),
  CONSTRAINT vulnerability_group_vulnerability_fk FOREIGN KEY (vulnerability_group_id) REFERENCES vulnerability_group (vulnerability_group_id),
  CONSTRAINT vulnerability_group_vulnerability_uk UNIQUE (vulnerability_group_id, vulnerability_refid)
);

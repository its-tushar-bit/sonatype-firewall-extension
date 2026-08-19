-- Since 1.98
CREATE TABLE search_index_change (
  search_index_change_id varchar(50) NOT NULL,
  change_type varchar(100) NOT NULL,
  change_data varchar(2000) NOT NULL,
  CONSTRAINT search_index_change_pk PRIMARY KEY (search_index_change_id)
);

-- Since 1.96
CREATE TABLE product_license (
  product_license_id varchar(50) NOT NULL,
  license_key varchar(8192) NOT NULL,
  license_details varchar(8192),
  CONSTRAINT product_license_pk PRIMARY KEY (product_license_id)
);

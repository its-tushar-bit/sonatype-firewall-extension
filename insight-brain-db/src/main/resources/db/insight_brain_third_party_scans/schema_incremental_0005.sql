ALTER TABLE third_party_file DROP COLUMN image;
ALTER TABLE third_party_file DROP COLUMN hash;
ALTER TABLE third_party_file ALTER COLUMN filename VARCHAR(1000) NULL;
ALTER TABLE third_party_file ADD COLUMN identification_source VARCHAR(100) NOT NULL;

ALTER TABLE file_coordinate DROP COLUMN source;
ALTER TABLE file_coordinate ADD COLUMN package_url VARCHAR(1000) ;

ALTER TABLE file_coordinate DROP CONSTRAINT file_coordinate_uk;

ALTER TABLE coordinate_security ADD COLUMN vulnerability_source VARCHAR(100) ;
ALTER TABLE coordinate_security ADD COLUMN severity_description VARCHAR(15) ;
ALTER TABLE coordinate_security ADD COLUMN attack_vector VARCHAR(100) ;
ALTER TABLE coordinate_security ADD COLUMN rating_method VARCHAR(10) ;
ALTER TABLE coordinate_security ADD COLUMN cwes TEXT ;
ALTER TABLE coordinate_security ADD COLUMN recommendations TEXT ;
ALTER TABLE coordinate_security ADD COLUMN advisories TEXT ;
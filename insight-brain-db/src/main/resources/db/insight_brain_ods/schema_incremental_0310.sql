-- Since 1.167
-- SaaS Compatible

UPDATE repository SET format='apk' WHERE repository_type='hosted' AND format='alpine';
UPDATE repository SET format='apt' WHERE repository_type='hosted' AND format='deb';
UPDATE repository SET format='go' WHERE repository_type='hosted' AND format='golang';
UPDATE repository SET format='maven2' WHERE repository_type='hosted' AND format='maven';
UPDATE repository SET format='r' WHERE repository_type='hosted' AND format='cran';
UPDATE repository SET format='rubygems' WHERE repository_type='hosted' AND format='gem';

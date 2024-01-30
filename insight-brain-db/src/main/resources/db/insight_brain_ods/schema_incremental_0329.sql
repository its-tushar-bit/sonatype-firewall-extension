-- Since 1.172
-- SaaS Compatible

ALTER TABLE perpetual_lock ADD COLUMN category varchar(50) NOT NULL DEFAULT 'source-control';

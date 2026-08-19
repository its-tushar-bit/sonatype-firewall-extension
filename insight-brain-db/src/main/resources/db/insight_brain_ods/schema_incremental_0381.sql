-- since 1.189
-- SaaS Compatible
ALTER TABLE policy_waiver ADD COLUMN is_for_container_image boolean DEFAULT false;
ALTER TABLE policy_waiver ADD COLUMN is_for_container_image_component boolean DEFAULT false;

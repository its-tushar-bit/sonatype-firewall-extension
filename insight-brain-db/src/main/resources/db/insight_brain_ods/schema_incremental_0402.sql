-- SaaS Compatible

ALTER TABLE organization ADD COLUMN related_repository_container_id varchar(50);

UPDATE organization
SET related_repository_container_id = 'REPOSITORY_CONTAINER_ID'
WHERE organization_id = (SELECT related_organization_id
                           FROM repository_container);

UPDATE organization
SET name = related_repository_manager_id,
    name_lowercase_no_whitespace = related_repository_manager_id
WHERE related_repository_manager_id IS NOT NULL;

UPDATE organization
SET name = related_repository_id,
    name_lowercase_no_whitespace = related_repository_id
WHERE related_repository_id IS NOT NULL;

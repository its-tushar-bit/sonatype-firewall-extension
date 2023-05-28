-- Since 1.162
CREATE TABLE source_control_organization_import_event
(
    source_control_organization_import_event_id VARCHAR(50)   NOT NULL,
    organization_id                             varchar(50)   NOT NULL,
    source_control_host_url                     VARCHAR(2048) NOT NULL,
    desired_sub_organization_count              INT           NOT NULL,
    import_limit                                INT           NOT NULL,
    import_status                               VARCHAR(20)   NOT NULL,
    import_success_count                        INT           NOT NULL,
    import_failure_count                        INT           NOT NULL,
    start_time                                  timestamp     NOT NULL,
    last_updated_time                           timestamp     NOT NULL,
    import_errors                               TEXT,
    CONSTRAINT source_control_organization_import_event_id PRIMARY KEY (source_control_organization_import_event_id),
    CONSTRAINT source_control_organization_import_event_organization_fk FOREIGN KEY (organization_id) REFERENCES organization (organization_id)
);

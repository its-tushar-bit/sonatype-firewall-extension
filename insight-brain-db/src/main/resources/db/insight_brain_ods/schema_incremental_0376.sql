-- since 1.189
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS component_change_detection_configuration (
    component_change_detection_configuration_id VARCHAR(50) NOT NULL,
    version VARCHAR(50) NOT NULL,
    purl VARCHAR(1000) NOT NULL,
    component_hash VARCHAR(50),
    comparison_hash VARCHAR(50),
    added_time TIMESTAMP NOT NULL,
    CONSTRAINT component_change_detection_configuration_pk PRIMARY KEY (component_change_detection_configuration_id),
    CONSTRAINT component_change_detection_configuration_uk UNIQUE (purl)
    );

CREATE INDEX component_change_detection_configuration_purl_idx ON component_change_detection_configuration(purl);
CREATE INDEX component_change_detection_configuration_added_time_idx ON component_change_detection_configuration(added_time);

-- since 1.189
CREATE TABLE IF NOT EXISTS component_change_detection_event (
    component_change_detection_event_id VARCHAR(50) NOT NULL,
    purl VARCHAR(1000) NOT NULL,
    component_evaluation_data TEXT NOT NULL,
    added_time TIMESTAMP NOT NULL,
    CONSTRAINT component_change_detection_event_pk PRIMARY KEY (component_change_detection_event_id)
    );

CREATE INDEX component_change_detection_event_purl_idx ON component_change_detection_event(purl);
CREATE INDEX component_change_detection_event_added_time_idx ON component_change_detection_event(added_time);

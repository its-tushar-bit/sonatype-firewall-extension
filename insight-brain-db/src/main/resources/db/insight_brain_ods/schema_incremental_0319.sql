-- Since 1.170
-- SaaS Compatible
CREATE TABLE sast_scan
(
    sast_scan_id                   varchar(50) NOT NULL,
    application_id                 varchar(50) NOT NULL,
    created_at                     timestamp NOT NULL,
    CONSTRAINT sast_scan_pk PRIMARY KEY (sast_scan_id),
    CONSTRAINT sast_scan_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id)
);

CREATE TABLE sast_finding
(
    sast_finding_id                varchar(50) NOT NULL,
    sast_scan_id                   varchar(50) NOT NULL,
    coordinate                     varchar(2000) NOT NULL,
    line_number                    integer,
    cwe                            varchar(50) NOT NULL,
    severity                       smallint NOT NULL,
    confidence                     smallint NOT NULL,
    rule_name                      varchar(200) NOT NULL,
    description                    text NOT NULL,
    CONSTRAINT sast_finding_pk PRIMARY KEY (sast_finding_id),
    CONSTRAINT sast_finding_sast_scan_fk FOREIGN KEY (sast_scan_id) REFERENCES sast_scan(sast_scan_id)
);

CREATE TABLE sast_remediation
(
    sast_remediation_id            varchar(50) NOT NULL,
    sast_finding_id                varchar(50) NOT NULL,
    content                        text NOT NULL,
    CONSTRAINT sast_remediation_pk PRIMARY KEY (sast_remediation_id),
    CONSTRAINT sast_remediation_sast_finding_fk FOREIGN KEY (sast_finding_id) REFERENCES sast_finding(sast_finding_id)
);

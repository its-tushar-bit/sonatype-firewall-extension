-- since 1.192
-- SaaS Compatible
CREATE TABLE zscaler_metrics (
    zscaler_metrics_id varchar(50) NOT NULL,

    maven_urls_from_hds INTEGER,
    npm_urls_from_hds INTEGER,
    pypi_urls_from_hds INTEGER,
    nuget_urls_from_hds INTEGER,

    maven_urls_to_zscaler INTEGER,
    npm_urls_to_zscaler INTEGER,
    pypi_urls_to_zscaler INTEGER,
    nuget_urls_to_zscaler INTEGER,

    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT zscaler_metrics_pk PRIMARY KEY (zscaler_metrics_id)
);

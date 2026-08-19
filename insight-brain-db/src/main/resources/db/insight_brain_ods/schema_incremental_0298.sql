-- Since 1.162
CREATE TABLE deleted_tenant
(
    tenant_slug                varchar(61) NOT NULL,
    delete_requested_timestamp bigint      NOT NULL,
    CONSTRAINT deleted_tenant_pk PRIMARY KEY (tenant_slug)
);

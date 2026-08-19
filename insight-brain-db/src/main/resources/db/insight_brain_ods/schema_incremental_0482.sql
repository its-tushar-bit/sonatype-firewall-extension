-- SaaS Compatible
-- FIRE-660: Data layer for Virtual Repository Managers (IQ as Redirector).
--
--  * Introduces a per-repository satellite `virtual_repository_config` that holds
--    ecosystem-specific configuration for repositories owned by a Virtual
--    Repository Manager (manager_type = 'VIRTUAL'):
--
--      protocol_version       varchar(50)   Ecosystem-neutral protocol/version
--                                            discriminator (initial writer: NuGet
--                                            v2/v3; the column stays generic so
--                                            other formats can reuse it. Backend
--                                            validation constrains legal values
--                                            per format).
--      pypi_package_host_url  varchar(2048) PyPI upstream + package-host URL pair
--                                            (format-scoped column name)
--      upstream_url           varchar(2048) proxy upstream URL (relocated from
--                                      `repository` — the only writer is behind
--                                      IQ_PROXY_ENABLED which is default-false,
--                                      so no deployed row holds a value and the
--                                      column can be dropped from `repository`
--                                      without data migration; see the DROP
--                                      COLUMN comment below)
--
--    Satellite table (rather than columns on `repository`) matches the
--    per-repository-type convention already used in this table family
--    (`proxy_repository_component`, `hosted_repository_component`) and keeps
--    the shared `repository` table free of ecosystem-specific NULL columns.
--
--  * `repository_manager.manager_type` becomes NOT NULL with DEFAULT 'TRADITIONAL'.
--    Existing NULL rows (from schema_incremental_0465 which added the column as
--    nullable with no default) are backfilled deterministically to 'TRADITIONAL'
--    in one statement. The same UPDATE also coerces any non-enum string value to
--    'TRADITIONAL' — a defensive measure for self-hosted deployments where a
--    direct-SQL write could have persisted a value outside {TRADITIONAL, VIRTUAL}.
--    No released version writes such values (the REST layer validates via the
--    ManagerType enum and the JPA entity uses @Enumerated(EnumType.STRING), so a
--    row with a garbage value is already unreadable through jOOQ today), so this
--    branch is a no-op on any healthy DB and a self-heal on a pathological one.
--    `repository_manager` holds a handful of rows per tenant schema (verified
--    against SaaS deployments), so the backfill + subsequent NOT NULL scan fit
--    comfortably inside the ECS migration budget. This matches the precedent
--    set by `schema_incremental_0287.sql` for `repository.repository_type` — the
--    directly analogous discriminator.
--
--  * `repository_manager_name_uk` moves from
--        UNIQUE (name_lowercase_no_whitespace)
--    to a plain composite
--        UNIQUE (name_lowercase_no_whitespace, manager_type)
--    so a Virtual Repository Manager and a traditional Repository Manager can
--    share a display name while still enforcing per-bucket uniqueness. With
--    `manager_type` NOT NULL, the plain composite is sufficient — no COALESCE
--    functional index is needed, and the same DDL works on both PostgreSQL and
--    H2 (hence a single dialect-agnostic migration file).

CREATE TABLE IF NOT EXISTS virtual_repository_config (
                                                         virtual_repository_config_id varchar(50)   NOT NULL,
    repository_id                varchar(50)   NOT NULL,
    protocol_version             varchar(50),
    pypi_package_host_url        varchar(2048),
    upstream_url                 varchar(2048),
    CONSTRAINT virtual_repository_config_pk PRIMARY KEY (virtual_repository_config_id),
    CONSTRAINT virtual_repository_config_repository_fk
    FOREIGN KEY (repository_id) REFERENCES repository(repository_id) ON DELETE CASCADE,
    CONSTRAINT virtual_repository_config_uk UNIQUE (repository_id)
    );

-- Drop `repository.upstream_url` (added in schema_incremental_0465). The only writer is
-- ApiFirewallService.addRepository which rejects non-VIRTUAL managers and sits behind
-- @HasFeature(IQ_PROXY_ENABLED), default false — so no deployed row holds a value and the
-- DROP needs no INSERT..SELECT copy. STL sign-off on 2026-08-03 confirmed the single-step
-- DROP is acceptable for this deployment (the standard SaaS "keep-nullable-one-release-then-drop"
-- pattern for column removal is being waived here because there is no data on disk to lose
-- and no read path outside the default-false feature-flagged branch — see FIRE-660
-- (STL sign-off 2026-08-03)).
ALTER TABLE repository DROP COLUMN IF EXISTS upstream_url;

-- Statements below are NOT atomic. Incremental scripts are executed by Spring's
-- ResourceDatabasePopulator on a DBCP2 pooled connection whose defaultAutoCommit is left at
-- the driver default (true), and H2 additionally commits implicitly after every DDL statement.
-- So each UPDATE / ALTER commits on its own, opening two theoretical windows:
--   (1) between the UPDATE backfill and SET NOT NULL — a concurrent insert of a NULL
--       manager_type on an older app version would cause SET NOT NULL to fail;
--   (2) between DROP CONSTRAINT and ADD CONSTRAINT — no name uniqueness for a few ms.
-- Neither window is reachable in our deployment topology: MTIQ runs migrations in an init
-- container while the app container is stopped, and self-hosted runs migrations at startup
-- before Jetty accepts traffic. `ClusterLockManager` serializes migrations across nodes so
-- two runners can't race on the same schema. If SET NOT NULL somehow does fail, the script
-- is idempotent — the next boot re-runs it and lands the same end state.
--
-- `sql-saas-compatibility.md` lists "Adding a NOT NULL constraint to an existing column"
-- under Unsafe Operations, waived here per FIRE-660 STL sign-off (2026-08-03; same waiver
-- reference used by the DROP COLUMN above) because `repository_manager` holds a handful of
-- rows per tenant schema and is not on the known-large-tables list. Do not generalize this
-- waiver to a table that fails any of those three properties (tiny row count, migration-time
-- writer quiescence, idempotent backfill).
UPDATE repository_manager
SET manager_type = 'TRADITIONAL'
WHERE manager_type IS NULL OR manager_type NOT IN ('TRADITIONAL', 'VIRTUAL');
ALTER TABLE repository_manager ALTER COLUMN manager_type SET DEFAULT 'TRADITIONAL';
ALTER TABLE repository_manager ALTER COLUMN manager_type SET NOT NULL;

ALTER TABLE repository_manager DROP CONSTRAINT IF EXISTS repository_manager_name_uk;
ALTER TABLE repository_manager
    ADD CONSTRAINT repository_manager_name_uk UNIQUE (name_lowercase_no_whitespace, manager_type);

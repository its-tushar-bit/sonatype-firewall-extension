# Database Migration Guide for Insight Brain

## Overview

This guide provides step-by-step instructions and best practices for making data structure changes within the Insight Brain project. The project uses a custom migration framework (not Liquibase or Flyway) with numbered incremental SQL scripts.

## Database Structure

### Data Stores

The insight-brain project uses four separate database schemas:

1. **insight_brain_ods** (Operational Data Store)
   - Main application database
   - Contains: organizations, applications, policies, users, violations, repositories, etc.
   - Location: `insight-brain-db/src/main/resources/db/insight_brain_ods/`

2. **insight_brain_dm** (Data Mart)
   - Reference data and lookups
   - Contains: licenses, multi-licenses, component categories, firewall patterns
   - Location: `insight-brain-db/src/main/resources/db/insight_brain_dm/`

3. **insight_brain_aggregation** (Aggregation)
   - Analytics and metrics aggregation
   - Contains: policy violation aggregations, time-series data
   - Location: `insight-brain-db/src/main/resources/db/insight_brain_aggregation/`

4. **insight_brain_third_party_scans** (Third Party Scans)
   - External scan data
   - Location: `insight-brain-db/src/main/resources/db/insight_brain_third_party_scans/`

### Database Engine Support

Both PostgreSQL (production) and H2 (development/testing) are supported. Schema changes must work on both databases.

## Migration Framework

### How It Works

1. **Schema Versioning**: Each data store tracks its version in the `schema_version` table
2. **Base Schema**: `schema.sql` contains the initial database structure
3. **Incremental Migrations**: Numbered files (`schema_incremental_NNNN.sql`) apply changes sequentially
4. **Script Registry**: `scripts.txt` lists all SQL files to execute in order
5. **Custom Migrator**: `DatabaseSchemaPopulator` class reads and executes scripts

## Step-by-Step Migration Process

### 1. Determine Which Data Store to Modify

Choose the appropriate data store based on the type of data:
- Application/business data → `insight_brain_ods`
- Reference/lookup data → `insight_brain_dm`
- Analytics/metrics → `insight_brain_aggregation`
- Third-party scan data → `insight_brain_third_party_scans`

### 2. Create a New Migration File

**Naming Convention**: `schema_incremental_NNNN.sql`

Where NNNN is the next sequential number (pad with leading zeros to 4 digits).

**Example**:
```bash
# Find the latest migration number
cd insight-brain-db/src/main/resources/db/insight_brain_ods/
ls schema_incremental_*.sql | tail -1
# Returns: schema_incremental_0419.sql

# Create the next one
touch schema_incremental_0420.sql
```

### 3. Write Your Migration SQL

#### Migration File Structure

```sql
-- Since 1.XXX
-- SaaS Compatible

-- Your migration SQL here
```

**Required Header Elements**:
- `-- Since 1.XXX`: Version when this was introduced (e.g., `-- Since 1.197`)
- `-- SaaS Compatible`: **REQUIRED** for all migrations since version 1.163

#### SaaS Compatibility Requirements

**Since version 1.163, ALL schema changes MUST be SaaS compatible.**

See: https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368/SaaS+Friendly+IQ+Database+Migrations

**Key Principles**:
- **No downtime**: Changes must allow old and new code to run simultaneously
- **Backward compatible**: Old code must work with new schema
- **Forward compatible**: New code must work with old schema
- **Idempotent**: Safe to run multiple times

**SaaS-Compatible Patterns**:

✅ **SAFE Operations**:
```sql
-- Adding nullable columns
ALTER TABLE my_table ADD COLUMN new_col VARCHAR(50) NULL;

-- Adding columns with defaults
ALTER TABLE my_table ADD COLUMN status VARCHAR(20) DEFAULT 'active';

-- Creating new tables
CREATE TABLE IF NOT EXISTS new_table (...);

-- Creating indexes
CREATE INDEX IF NOT EXISTS idx_name ON table_name(column);

-- Adding columns conditionally (PostgreSQL-compatible)
ALTER TABLE my_table ADD COLUMN IF NOT EXISTS new_col VARCHAR(50);

-- Dropping NOT NULL constraint
ALTER TABLE my_table ALTER COLUMN col_name DROP NOT NULL;
```

❌ **UNSAFE Operations** (require multi-step migration):
```sql
-- Adding NOT NULL columns without default
ALTER TABLE my_table ADD COLUMN new_col VARCHAR(50) NOT NULL;

-- Dropping columns
ALTER TABLE my_table DROP COLUMN old_col;

-- Renaming columns
ALTER TABLE my_table RENAME COLUMN old_name TO new_name;

-- Changing column types
ALTER TABLE my_table ALTER COLUMN col_name TYPE INTEGER;

-- Adding NOT NULL constraint
ALTER TABLE my_table ALTER COLUMN col_name SET NOT NULL;
```

#### Multi-Step Migration Pattern

For unsafe operations, use multiple migration files:

**Step 1** (schema_incremental_0420.sql):
```sql
-- Since 1.197
-- SaaS Compatible

-- Add new column as nullable
ALTER TABLE my_table ADD COLUMN new_required_col VARCHAR(50) NULL;
```

**Step 2** (after deploying code that populates the column):
```sql
-- Since 1.198
-- SaaS Compatible

-- Add NOT NULL constraint after column is populated
-- Note: Ensure all existing rows have values before running
ALTER TABLE my_table ALTER COLUMN new_required_col SET NOT NULL;
```

**Step 3** (for column removal, after old code is fully retired):
```sql
-- Since 1.199
-- SaaS Compatible

-- Safe to drop now that old code is gone
ALTER TABLE my_table DROP COLUMN old_col;
```

### 4. Common Patterns and Examples

#### Adding a New Table

```sql
-- Since 1.197
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS my_new_table (
  my_table_id VARCHAR(50) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT my_new_table_pk PRIMARY KEY (my_table_id)
);

CREATE INDEX my_new_table_name_idx ON my_new_table(name);
```

#### Adding a Column

```sql
-- Since 1.197
-- SaaS Compatible

-- Add nullable column (safe)
ALTER TABLE application ADD COLUMN tags_json TEXT NULL;

-- Or with conditional (PostgreSQL-friendly)
ALTER TABLE application ADD COLUMN IF NOT EXISTS tags_json TEXT NULL;
```

#### Modifying Constraints

```sql
-- Since 1.197
-- SaaS Compatible

-- Dropping NOT NULL is safe
ALTER TABLE my_table ALTER COLUMN optional_field DROP NOT NULL;
ALTER TABLE my_table ALTER COLUMN optional_field DROP DEFAULT;
```

#### Adding Indexes

```sql
-- Since 1.197
-- SaaS Compatible

CREATE INDEX policy_violation_component_hash_idx ON policy_violation(component_hash);
CREATE INDEX application_org_id_idx ON application(organization_id);
```

#### Adding Foreign Keys

```sql
-- Since 1.197
-- SaaS Compatible

ALTER TABLE child_table ADD CONSTRAINT child_parent_fk
  FOREIGN KEY (parent_id) REFERENCES parent_table(parent_id)
  ON DELETE CASCADE;
```

#### Inserting Reference Data

```sql
-- Since 1.197
-- SaaS Compatible

INSERT INTO system_configuration_property
  (system_configuration_property_id, name, value)
VALUES
  ('newfeature123', 'NEW_FEATURE_ENABLED', 'false');
```

### 5. Update the Scripts Registry

Add your new migration file to the `scripts.txt` file in the same directory:

**File**: `insight-brain-db/src/main/resources/db/insight_brain_ods/scripts.txt`

```txt
schema.sql
schema_incremental_0172.sql
schema_incremental_0173.sql
...
schema_incremental_0419.sql
schema_incremental_0420.sql
```

**Important**: The order matters! Files are executed in the order listed.

### 6. Update Entity Classes (if applicable)

If you're adding/modifying tables that correspond to JPA entities:

**Location**: `insight-brain-db/src/main/java/com/sonatype/insight/brain/hds/entity/`

**Example**:
```java
@Entity
@Table(name = "my_table")
public class MyEntity {

  @Id
  @Column(name = "my_table_id")
  private String id;

  @Column(name = "new_column", nullable = true)
  private String newColumn;

  // getters, setters...
}
```

### 7. Create Data Access Objects (if needed)

**Location**: `insight-brain-data/src/main/java/com/sonatype/insight/brain/data/`

### 8. Testing Your Migration

#### Local Testing

```bash
# Build with quick profile
mvn clean install -Pquick

# Run the application (triggers migration)
cd insight-brain-service
mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.spring.InsightBrainSpringApplication \
  -Dexec.args='server src/test/resources/config-dev.yml'
```

#### Test Database Cleanup

For tests that use the database, use the `TemporaryEntity` rule:

```java
@Rule
public TemporaryEntity tempEntity = new TemporaryEntity();
```

#### Verify Migration

Check the `schema_version` table:
```sql
SELECT * FROM schema_version WHERE data_store_id = 'insight_brain_ods';
```

### 9. Handle Database-Specific Syntax

Use compatible SQL that works on both PostgreSQL and H2:

**PostgreSQL-Specific Files** (when absolutely necessary):
- Create: `scripts_postgresql.txt` (alongside `scripts.txt`)
- Add PostgreSQL-specific scripts there

**H2-Specific Files** (when absolutely necessary):
- Create: `scripts_h2.txt`
- Add H2-specific scripts there

**Example** (insight_brain_ods):
```
scripts.txt              # Common scripts
scripts_postgresql.txt   # PostgreSQL-only scripts
scripts_h2.txt          # H2-only scripts
```

The framework automatically picks the right file based on the database engine.

## Common Patterns by Use Case

### Adding a Feature Flag

```sql
-- Since 1.197
-- SaaS Compatible

INSERT INTO system_configuration_property
  (system_configuration_property_id, name, value)
VALUES
  ('unique-guid-here', 'MY_FEATURE_ENABLED', 'false');
```

### Adding Application Metadata

```sql
-- Since 1.197
-- SaaS Compatible

ALTER TABLE application ADD COLUMN metadata_json TEXT NULL;
CREATE INDEX application_metadata_idx ON application(metadata_json);
```

### Creating a Join Table

```sql
-- Since 1.197
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS entity_tag (
  entity_tag_id VARCHAR(50) NOT NULL,
  entity_id VARCHAR(50) NOT NULL,
  tag_id VARCHAR(50) NOT NULL,
  CONSTRAINT entity_tag_pk PRIMARY KEY (entity_tag_id),
  CONSTRAINT entity_tag_uk UNIQUE (entity_id, tag_id),
  CONSTRAINT entity_tag_entity_fk FOREIGN KEY (entity_id)
    REFERENCES entity(entity_id) ON DELETE CASCADE,
  CONSTRAINT entity_tag_tag_fk FOREIGN KEY (tag_id)
    REFERENCES tag(tag_id) ON DELETE CASCADE
);

CREATE INDEX entity_tag_entity_id_idx ON entity_tag(entity_id);
CREATE INDEX entity_tag_tag_id_idx ON entity_tag(tag_id);
```

### Adding Audit Columns

```sql
-- Since 1.197
-- SaaS Compatible

ALTER TABLE my_table ADD COLUMN created_by VARCHAR(60) NULL;
ALTER TABLE my_table ADD COLUMN created_at TIMESTAMP NULL;
ALTER TABLE my_table ADD COLUMN updated_by VARCHAR(60) NULL;
ALTER TABLE my_table ADD COLUMN updated_at TIMESTAMP NULL;
```

## Advanced: Data Migrations

For complex data transformations, use Java-based migrations:

**Location**: `insight-brain-db/src/main/java/com/sonatype/insight/brain/db/migrations/`

**Example**:
```java
public class MyDataMigration extends AbstractDatabaseMigrator {

  @Override
  protected void migrate(DataStore dataStore) {
    // Perform data transformation
    JdbcTemplate jdbc = new JdbcTemplate(dataStore.getDataSource());
    jdbc.update("UPDATE my_table SET new_col = old_col WHERE new_col IS NULL");
  }
}
```

Register in `DatabaseMigrators` class and add tracking to `migration_tracker` table.

## Migration Tracker

The `migration_tracker` table tracks one-time data migrations:

```sql
CREATE TABLE migration_tracker (
    migration_tracker_id VARCHAR(100) NOT NULL,
    version INT NULL,
    configuration VARCHAR(1000) NULL,
    CONSTRAINT migration_tracker_pk PRIMARY KEY (migration_tracker_id)
);
```

**Usage**:
```sql
-- Check if migration already ran
SELECT * FROM migration_tracker WHERE migration_tracker_id = 'my-migration-name';

-- Record that migration ran
INSERT INTO migration_tracker(migration_tracker_id, version)
VALUES('my-migration-name', 1);
```

## Naming Conventions

### Tables
- Lowercase with underscores: `policy_violation`, `source_control_event`
- Descriptive names: avoid abbreviations unless standard (e.g., `sv` for security vulnerability)

### Columns
- Lowercase with underscores: `application_id`, `created_at`
- Primary keys: `{table_name}_id`
- Foreign keys: `{referenced_table}_id`
- Boolean columns: Use `_enabled`, `_allowed`, or `is_` prefix

### Constraints
- Primary keys: `{table_name}_pk`
- Foreign keys: `{table_name}_{referenced_table}_fk`
- Unique constraints: `{table_name}_{column}_uk` or `{table_name}_uk`
- Indexes: `{table_name}_{column}_idx`

### IDs
- Use VARCHAR(50) for all ID columns
- Generate UUIDs at the application layer
- Use meaningful IDs for system records (e.g., `'ROOT_ORGANIZATION_ID'`)

## Common Pitfalls

### ❌ Don't Do This

```sql
-- Missing SaaS Compatible comment
ALTER TABLE my_table ADD COLUMN foo VARCHAR(50);

-- Adding NOT NULL without default in one step
ALTER TABLE my_table ADD COLUMN required_col VARCHAR(50) NOT NULL;

-- Dropping columns immediately
ALTER TABLE my_table DROP COLUMN old_col;

-- Not using IF EXISTS/IF NOT EXISTS
CREATE TABLE my_table (...);
ALTER TABLE my_table ADD COLUMN foo VARCHAR(50);

-- Forgetting to update scripts.txt
-- (Your migration won't run!)
```

### ✅ Do This Instead

```sql
-- Since 1.197
-- SaaS Compatible

-- Add nullable first, make required later
ALTER TABLE my_table ADD COLUMN IF NOT EXISTS required_col VARCHAR(50) NULL;

-- Use conditional operations
CREATE TABLE IF NOT EXISTS my_table (...);
CREATE INDEX IF NOT EXISTS idx_name ON table_name(column);

-- Update scripts.txt
-- Add your file to the list!
```

## Checklist

Before committing your migration:

- [ ] Created `schema_incremental_NNNN.sql` with next sequential number
- [ ] Added `-- Since 1.XXX` comment
- [ ] Added `-- SaaS Compatible` comment (REQUIRED)
- [ ] Used SaaS-compatible SQL patterns
- [ ] Added `IF EXISTS` / `IF NOT EXISTS` where applicable
- [ ] Updated `scripts.txt` with new file
- [ ] Updated JPA entity classes (if applicable)
- [ ] Tested migration locally
- [ ] Verified schema_version incremented
- [ ] Checked PostgreSQL and H2 compatibility
- [ ] Updated corresponding DAO/repository classes

## Resources

- **SaaS Migration Guidelines**: https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368/SaaS+Friendly+IQ+Database+Migrations
- **Database Code Location**: `insight-brain-db/src/main/resources/db/`
- **Migration Java Code**: `insight-brain-db/src/main/java/com/sonatype/insight/brain/db/migrations/`
- **Entity Classes**: `insight-brain-db/src/main/java/com/sonatype/insight/brain/hds/entity/`

## Getting Help

If you have questions about database migrations:
1. Check existing migrations for similar patterns
2. Review the SaaS compatibility documentation
3. Ask in the team Slack channel
4. Consult with senior developers for complex changes

## Version History

- **1.163+**: SaaS Compatible migrations required
- **1.104**: aggregate_file table added
- **1.176**: organization_ancestor table for hierarchy queries
- **1.177**: OAuth2/OIDC support
- **1.178**: Development prioritization tables
- **1.183**: Auto policy waiver support
- **1.190**: Cluster identification
- **1.193**: CPE matching configuration
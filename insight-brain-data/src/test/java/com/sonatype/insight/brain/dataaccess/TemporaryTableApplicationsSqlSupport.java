/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

/**
 * Raw SQL builders that seed and clean up a large batch of applications directly in the operational data store,
 * used by Postgres component tests that need to exceed the {@link AbstractSqlDAO#POSTGRES_IN_OPERATOR_THRESHOLD}
 * to exercise the temporary-table query path.
 *
 * <p>
 * Published in the {@code insight-brain-data} test-jar (see the module {@code maven-jar-plugin} {@code <includes>})
 * so both the relocated Postgres dashboard component tests and the temporary-table helper's own coverage can share
 * them without depending on a Postgres-only test class.
 */
public final class TemporaryTableApplicationsSqlSupport
{
  private TemporaryTableApplicationsSqlSupport() {
  }

  public static String getInsertMaximumApplicationsSql(final String organizationId) {
    return """
        INSERT INTO insight_brain_ods.application (application_id, public_id, public_id_lowercase, name,
            name_lowercase_no_whitespace, organization_id)
        SELECT  REPLACE(gen_random_uuid()::text, '-', '') AS application_id,
        'application-' || g.id AS public_id,
        'application-' || g.id AS public_id_lowercase,
        'Application ' || g.id AS name,
        'application' || g.id AS name_lowercase_no_whitespace,
        '%s' AS organization_id
        FROM generate_series(1, %s) AS g (id)""".formatted(organizationId,
        AbstractSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD);
  }

  public static String getCleanupApplicationsSql() {
    return "DELETE FROM insight_brain_ods.application WHERE public_id LIKE 'application-%'";
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RootOrganizationConfigMigrationUtilsTest
    extends MigratorTest
{
  @Test
  public void testSetMigrated() {
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
    assertThat(migrationUtils.isMigrated()).isFalse();

    migrationUtils.setMigrated();

    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }

  @Test
  public void testSetSourceOrganizationId_And_SetMigrated() {
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
    assertThat(migrationUtils.isMigrated()).isFalse();
    String sourceOrganizationId = "bla";

    migrationUtils.setSourceOrganizationId(sourceOrganizationId);

    assertThat(migrationUtils.getSourceOrganizationId()).isEqualTo(sourceOrganizationId);
    assertThat(migrationUtils.isMigrated()).isFalse();

    migrationUtils.setMigrated();

    assertThat(migrationUtils.isMigrationScheduled()).isTrue();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }
}

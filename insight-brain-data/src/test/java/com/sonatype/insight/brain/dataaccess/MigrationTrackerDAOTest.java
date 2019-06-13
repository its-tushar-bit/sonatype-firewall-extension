/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.MigrationTracker;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTrackerDAOTest
    extends AbstractDbDAOTest
{
  private MigrationTrackerDAO dao = new MigrationTrackerDAO();

  @Test
  public void testCRD() {
    MigrationTracker migrationTracker = new MigrationTracker();
    String aMigrationIdentifier = "PolicySomePropertyMigration";
    migrationTracker.setId(aMigrationIdentifier);
    migrationTracker.setVersion(null);
    migrationTracker.setConfiguration(null);
    dao.insert(migrationTracker);
    assertThat(dao.getById(aMigrationIdentifier)).isNotNull();
    assertThat(dao.getById(aMigrationIdentifier).getId()).isEqualTo(aMigrationIdentifier);
    assertThat(dao.getById(aMigrationIdentifier).getVersion()).isNull();
    assertThat(dao.getById(aMigrationIdentifier).getConfiguration()).isNull();
    dao.delete(migrationTracker);
    assertThat(dao.getById(aMigrationIdentifier)).isNull();
  }
}

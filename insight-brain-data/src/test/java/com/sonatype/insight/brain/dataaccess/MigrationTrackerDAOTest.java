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
    dao.insert(migrationTracker);
    assertThat(dao.getById(aMigrationIdentifier)).isNotNull();
    dao.delete(migrationTracker);
    assertThat(dao.getById(aMigrationIdentifier)).isNull();
  }
}

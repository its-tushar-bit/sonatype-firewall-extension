/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.MigrationTracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTrackerDAOTest
    extends AbstractDbDAOTest
{
  private MigrationTrackerDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createMigrationTrackerDAO();
  }

  @Test
  public void testCRUD() {
    String id = "id";

    MigrationTracker instance = new MigrationTracker(id);

    // Create
    dao.insert(instance);

    // Read
    MigrationTracker byId = dao.getById(id);
    assertThat(byId).isNotNull();
    assertThat(byId.getId()).isEqualTo(id);
    assertThat(byId.getVersion()).isNull();
    assertThat(byId.getConfiguration()).isNull();

    // Update
    instance.setVersion(1);
    instance.setConfiguration("Configuration");
    dao.update(instance);

    // Read Updated
    byId = dao.getById(id);
    assertThat(byId).isNotNull();
    assertThat(byId.getId()).isEqualTo(id);
    assertThat(byId.getVersion()).isEqualTo(1);
    assertThat(byId.getConfiguration()).isEqualTo("Configuration");

    // Delete
    dao.delete(instance);
    assertThat(dao.getById(id)).isNull();
  }
}

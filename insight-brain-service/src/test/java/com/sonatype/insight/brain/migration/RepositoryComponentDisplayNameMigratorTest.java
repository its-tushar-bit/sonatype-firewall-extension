/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryComponentDisplayNameMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private RepositoryComponentDisplayNameMigrator repositoryComponentDisplayNameMigrator;

  @Inject
  private OperationalDataStore operationalDataStore;

  @Test
  public void testMigrate() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(), new Date(), null);
    nullifyDisplayNames();

    migrationTrackerDAO.deleteById(RepositoryComponentDisplayNameMigrator.MIGRATION_ID);

    repositoryComponentDisplayNameMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(RepositoryComponentDisplayNameMigrator.MIGRATION_ID)).isTrue();

    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());
    assertThat(repositoryComponent.getDisplayName()).isEqualTo("g1 : a1 : e1 : c1 : v1");
  }

  @Test
  public void testMigrate_AlreadyMigrated() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path1", "hash1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(), new Date(), null);
    nullifyDisplayNames();

    repositoryComponentDisplayNameMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(RepositoryComponentDisplayNameMigrator.MIGRATION_ID)).isTrue();

    // Since the migrator already run, it shouldn't have done anything, so the display names should still be null.
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());
    assertThat(repositoryComponent.getDisplayName()).isNull();
  }

  private void nullifyDisplayNames() throws SQLException {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement updateStmt =
            connection.prepareStatement("UPDATE " + operationalDataStore.getDatabaseSchema()
                + ".repository_component" + " SET display_name = NULL"))
    {
      updateStmt.execute();
    }
  }
}

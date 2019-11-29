/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.HashMap;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class IgnoredRepositoryComponentMigratorTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient hdsClientMock;

  @Inject
  private IgnoredRepositoryComponentMigrator ignoredRepositoryComponentMigrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClientMock);
    super.configure(binder);
  }

  @Test
  public void testMigrate() {
    new MigrationTrackerDAO().delete(new MigrationTracker(IgnoredRepositoryComponentMigrator.MIGRATION_ID));
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");
    tempEntity.newRepositoryComponent(repository, "a/sha", MatchState.UNKNOWN, "hash");

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), asList(".*sha$"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentMigrator.MIGRATION_ID)).isNull();
    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isNotEmpty();

    ignoredRepositoryComponentMigrator.migrate();

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentMigrator.MIGRATION_ID)).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
  }
}

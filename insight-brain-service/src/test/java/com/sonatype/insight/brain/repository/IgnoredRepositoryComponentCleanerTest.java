/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.HashMap;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class IgnoredRepositoryComponentCleanerTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient hdsClientMock;

  @Inject
  private IgnoredRepositoryComponentCleaner ignoredRepositoryComponentMigrator;

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
  public void testStart() {
    new MigrationTrackerDAO().deleteById(IgnoredRepositoryComponentCleaner.MIGRATION_ID);
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");
    tempEntity.newRepositoryComponent(repository, "a/sha", MatchState.UNKNOWN, "hash");

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), asList(".*sha$"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentCleaner.MIGRATION_ID)).isNull();
    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isNotEmpty();

    ignoredRepositoryComponentMigrator.start();

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentCleaner.MIGRATION_ID)).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  public void testStart_HdsError() {
    new MigrationTrackerDAO().deleteById(IgnoredRepositoryComponentCleaner.MIGRATION_ID);
    tempEntity.newRepository("rm1", "r1", "maven2");

    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenThrow(new BadGatewayException("ERROR"));

    ignoredRepositoryComponentMigrator.start();

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentCleaner.MIGRATION_ID)).isNull();
  }
}

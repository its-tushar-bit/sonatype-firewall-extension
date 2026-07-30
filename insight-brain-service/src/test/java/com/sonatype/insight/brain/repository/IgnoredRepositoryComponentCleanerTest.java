/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadGatewayException;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

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
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Test
  public void testStart_AlreadyMigrated() {
    ignoredRepositoryComponentMigrator.register();

    verifyNoInteractions(mockTaskScheduler);
  }

  @Test
  public void testStart_NotMigrated() {
    migrationTrackerDAO.deleteById(IgnoredRepositoryComponentCleaner.MIGRATION_ID);

    ignoredRepositoryComponentMigrator.register();

    verify(mockTaskScheduler).scheduleOneTimeTask(ignoredRepositoryComponentMigrator);
  }

  @Test
  public void testExecute() throws Exception {
    migrationTrackerDAO.deleteById(IgnoredRepositoryComponentCleaner.MIGRATION_ID);
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");
    tempEntity.newRepositoryComponent(repository, "a/sha", MatchState.UNKNOWN, "hash");

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put(repository.getFormat(), Collections.singletonList(".*sha$"));
    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentCleaner.MIGRATION_ID)).isNull();
    assertThat(proxyRepositoryComponentDAO.getByRepositoryId(repository.getId())).isNotEmpty();

    ignoredRepositoryComponentMigrator.execute(null);

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentCleaner.MIGRATION_ID)).isNotNull();
    assertThat(proxyRepositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  public void testExecute_HdsError() throws Exception {
    migrationTrackerDAO.deleteById(IgnoredRepositoryComponentCleaner.MIGRATION_ID);
    tempEntity.newRepository("rm1", "r1", "maven2");

    when(hdsClientMock.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenThrow(new BadGatewayException("ERROR"));

    ignoredRepositoryComponentMigrator.execute(null);

    assertThat(migrationTrackerDAO.getById(IgnoredRepositoryComponentCleaner.MIGRATION_ID)).isNull();
  }

  @Test
  public void testExecute_System() throws Exception {
    IgnoredRepositoryComponentCleaner spyIgnoredRepositoryComponentCleaner = spy(ignoredRepositoryComponentMigrator);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spyIgnoredRepositoryComponentCleaner).doDeleteIgnoredRepositoryComponents();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyIgnoredRepositoryComponentCleaner.execute(mock(JobExecutionContext.class));
    }

    verify(spyIgnoredRepositoryComponentCleaner).doDeleteIgnoredRepositoryComponents();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(
        JobBuilder.newJob(IgnoredRepositoryComponentCleaner.class).build().isConcurrentExectionDisallowed()).isTrue();
  }
}

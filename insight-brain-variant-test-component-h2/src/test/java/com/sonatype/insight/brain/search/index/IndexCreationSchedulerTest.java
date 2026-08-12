/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexHealthDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexJobDAO;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJob;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.MDC;

@ComponentH2Test
public class IndexCreationSchedulerTest
    extends AbstractComponentH2Test
{
  @Inject
  private IndexCreationScheduler indexCreationScheduler;

  @Inject
  private SearchIndexJobDAO jobDAO;

  @Inject
  private SearchIndexHealthDAO healthDAO;

  @Mock
  private IndexService mockIndexService;

  private final List<String> jobIds = new ArrayList<>();

  @AfterEach
  public void removeJobs() {
    jobIds.stream().map(jobDAO::getById).filter(job -> job != null).forEach(jobDAO::delete);
    jobIds.clear();
    healthDAO.setActiveJobId(null);
  }

  @Test
  public void testExecute() throws Exception {
    IndexCreationScheduler indexCreationSchedulerSpy = spy(indexCreationScheduler);

    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(mockIndexService).createSearchIndex();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      indexCreationSchedulerSpy.execute(null);
    }

    verify(mockIndexService).createSearchIndex();
  }

  /**
   * The completion hook is only worth anything if the engine actually calls it. Its own tests mock
   * the job lookup, and {@code SearchIndexJobDAOTest} proves that a terminal status frees the slot
   * for the next job, but neither covers the seam between them. A rebuild that runs to the end
   * without reporting back leaves its job RUNNING, and because {@code active_slot} is uniquely
   * constrained, every later start then conflicts until someone edits the database by hand.
   */
  @Test
  public void execute_closesTheActiveJobAndFreesTheSlot() throws Exception {
    SearchIndexJob running = insertRunningRebuild();

    indexCreationScheduler.execute(null);

    SearchIndexJob finished = jobDAO.getById(running.getId());
    assertThat(finished.getStatus()).isEqualTo(SearchIndexJob.STATUS_SUCCEEDED);
    assertThat(finished.getActiveSlot()).isNull();
    assertThat(jobDAO.findActiveJob()).isEmpty();
  }

  /**
   * A rebuild that throws has to close its job too, or it blocks the retry an operator reaches for
   * next. {@code execute} swallows the exception to keep the scheduler alive, so the job row is the
   * only place the failure is recorded.
   */
  @Test
  public void execute_recordsAFailedRebuildAndStillFreesTheSlot() throws Exception {
    doThrow(new IllegalStateException("index build blew up"))
        .when(mockIndexService)
        .createSearchIndex();
    SearchIndexJob running = insertRunningRebuild();

    indexCreationScheduler.execute(null);

    SearchIndexJob finished = jobDAO.getById(running.getId());
    assertThat(finished.getStatus()).isEqualTo(SearchIndexJob.STATUS_FAILED);
    assertThat(finished.getErrorMessage()).contains("index build blew up");
    assertThat(jobDAO.findActiveJob()).isEmpty();
  }

  /**
   * Writes the row {@code SearchIndexJobService#startJob} would have written. Going through the
   * service itself is not possible here: it asks {@code IndexService} whether a rebuild is already
   * running, which reads Quartz's job store, and this context has no running scheduler.
   */
  private SearchIndexJob insertRunningRebuild() {
    Date now = new Date();
    SearchIndexJob job = new SearchIndexJob();
    job.setId(UUID.randomUUID().toString().replace("-", ""));
    job.setJobType(SearchIndexJob.TYPE_FULL_REBUILD);
    job.setTrigger(SearchIndexJob.TRIGGER_HEALTH_UI);
    job.setStatus(SearchIndexJob.STATUS_RUNNING);
    job.setPhase("REBUILDING");
    job.setProgressPercent((short) 0);
    job.setCreatedByUserId(MDCUsernameScope.SYSTEM);
    job.setCreatedAt(now);
    job.setStartedAt(now);
    job.setUpdatedAt(now);
    jobDAO.insert(job);
    jobIds.add(job.getId());
    healthDAO.setActiveJobId(job.getId());
    return job;
  }
}

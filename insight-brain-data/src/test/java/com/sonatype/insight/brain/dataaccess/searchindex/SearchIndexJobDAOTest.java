/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJob;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SearchIndexJobDAOTest
    extends AbstractDbDAOTest
{
  private SearchIndexJobDAO dao;

  private final List<SearchIndexJob> inserted = new ArrayList<>();

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = new SearchIndexJobDAO(databaseRule.getOperationalDataStore());
  }

  @After
  public void removeInsertedJobs() {
    inserted.forEach(job -> dao.delete(job));
    inserted.clear();
  }

  /**
   * The service lock only covers one node, so the single-active-job invariant needs a backstop in the
   * database. active_slot carries a UNIQUE constraint and holds a constant while the job is active.
   */
  @Test
  public void insert_refusesASecondActiveJob() {
    insertJob("job-1", SearchIndexJob.STATUS_RUNNING);

    SearchIndexJob second = job("job-2", SearchIndexJob.STATUS_PENDING);

    assertThatThrownBy(() -> dao.insert(second)).isInstanceOf(RuntimeException.class);
  }

  /**
   * Terminal jobs release the slot by holding null, which neither dialect counts toward uniqueness, so
   * history accumulates without blocking the next job.
   */
  @Test
  public void insert_allowsManyTerminalJobsAlongsideOneActive() {
    insertJob("done-1", SearchIndexJob.STATUS_SUCCEEDED);
    insertJob("done-2", SearchIndexJob.STATUS_FAILED);
    insertJob("done-3", SearchIndexJob.STATUS_CANCELLED);
    insertJob("live-1", SearchIndexJob.STATUS_RUNNING);

    assertThat(dao.findActiveJob()).isPresent();
    assertThat(dao.findActiveJob().get().getId()).isEqualTo("live-1");
  }

  /** Finishing a job has to free the slot, or the install can never start another one. */
  @Test
  public void update_releasesTheSlotWhenAJobReachesATerminalStatus() {
    SearchIndexJob job = insertJob("job-1", SearchIndexJob.STATUS_RUNNING);

    job.setStatus(SearchIndexJob.STATUS_SUCCEEDED);
    job.setUpdatedAt(new Date());
    dao.update(job);

    assertThat(dao.findActiveJob()).isEmpty();
    assertThat(dao.getById("job-1").getActiveSlot()).isNull();
    insertJob("job-2", SearchIndexJob.STATUS_PENDING);
    assertThat(dao.findActiveJob()).isPresent();
  }

  /** CANCELLING is still active: the engine has not stopped, so the slot stays taken. */
  @Test
  public void update_keepsTheSlotWhileCancellationIsStillInFlight() {
    SearchIndexJob job = insertJob("job-1", SearchIndexJob.STATUS_RUNNING);

    job.setStatus(SearchIndexJob.STATUS_CANCELLING);
    job.setUpdatedAt(new Date());
    dao.update(job);

    assertThat(dao.getById("job-1").getActiveSlot()).isEqualTo(SearchIndexJob.ACTIVE_SLOT);
    assertThat(dao.findActiveJob()).isPresent();
  }

  private SearchIndexJob insertJob(final String id, final String status) {
    SearchIndexJob job = job(id, status);
    dao.insert(job);
    inserted.add(job);
    return job;
  }

  private SearchIndexJob job(final String id, final String status) {
    SearchIndexJob job = new SearchIndexJob();
    job.setId(id);
    job.setJobType(SearchIndexJob.TYPE_FULL_REBUILD);
    job.setTrigger(SearchIndexJob.TRIGGER_HEALTH_UI);
    job.setStatus(status);
    job.setProgressPercent((short) 0);
    job.setCreatedAt(new Date());
    job.setUpdatedAt(new Date());
    return job;
  }
}

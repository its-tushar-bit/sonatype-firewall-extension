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
import com.sonatype.insight.brain.model.searchindex.SearchIndexJobEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchIndexJobEventDAOTest
    extends AbstractDbDAOTest
{
  private SearchIndexJobEventDAO dao;

  private final List<SearchIndexJobEvent> inserted = new ArrayList<>();

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = new SearchIndexJobEventDAO(databaseRule.getOperationalDataStore());
  }

  @AfterEach
  public void removeInsertedEvents() {
    inserted.forEach(event -> dao.delete(event));
    inserted.clear();
  }

  @Test
  public void listByJobId_returnsEventsOldestFirst() {
    insertEvent("job-1", 1L, "STARTED");
    insertEvent("job-1", 2L, "ENGINE_STARTED");
    insertEvent("job-1", 3L, "SUCCEEDED");

    assertThat(dao.listByJobId("job-1", 100))
        .extracting(SearchIndexJobEvent::getEventCode)
        .containsExactly("STARTED", "ENGINE_STARTED", "SUCCEEDED");
  }

  /**
   * A truncated activity log has to lose its earliest lines, not its latest. The terminal and error
   * events are the last ones written and the whole reason to read the log.
   */
  @Test
  public void listByJobId_keepsTheNewestEventsWhenTruncated() {
    insertEvent("job-1", 1L, "STARTED");
    insertEvent("job-1", 2L, "ENGINE_STARTED");
    insertEvent("job-1", 3L, "REBUILD_FAILED");

    assertThat(dao.listByJobId("job-1", 2))
        .extracting(SearchIndexJobEvent::getEventCode)
        .containsExactly("ENGINE_STARTED", "REBUILD_FAILED");
  }

  @Test
  public void listByJobId_ignoresOtherJobs() {
    insertEvent("job-1", 1L, "STARTED");
    insertEvent("job-2", 1L, "STARTED");

    assertThat(dao.listByJobId("job-1", 100)).hasSize(1);
  }

  private void insertEvent(final String jobId, final long seq, final String eventCode) {
    SearchIndexJobEvent event = new SearchIndexJobEvent();
    event.setId(jobId + "-" + seq);
    event.setSearchIndexJobId(jobId);
    event.setSeq(seq);
    event.setSeverity(SearchIndexJobEvent.SEVERITY_INFO);
    event.setEventCode(eventCode);
    event.setMessage(eventCode + " message");
    event.setCreatedAt(new Date());
    dao.insert(event);
    inserted.add(event);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJobEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SearchIndexJobEvent.SEARCH_INDEX_JOB_EVENT;

@Named
@Singleton
public class SearchIndexJobEventDAO
    extends AbstractOperationalSqlDAO<SearchIndexJobEvent>
{
  @Inject
  public SearchIndexJobEventDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * The most recent {@code limit} events for a job, oldest first.
   * <p>
   * The limit is applied to a descending scan and the page reversed afterwards, so a job with more
   * events than the caller asked for loses its earliest lines rather than its latest. Ascending plus
   * a limit would truncate the other end and hide the terminal and error lines, which are the ones
   * an activity log exists to show.
   */
  public List<SearchIndexJobEvent> listByJobId(final String jobId, final int limit) {
    try (TransactionContext tx = createTransactionContext()) {
      List<SearchIndexJobEvent> newestFirst = tx.dsl()
          .selectFrom(SEARCH_INDEX_JOB_EVENT)
          .where(SEARCH_INDEX_JOB_EVENT.SEARCH_INDEX_JOB_ID.eq(jobId))
          .orderBy(SEARCH_INDEX_JOB_EVENT.SEQ.desc())
          .limit(limit)
          .fetchInto(SearchIndexJobEvent.class);
      Collections.reverse(newestFirst);
      return newestFirst;
    }
  }

  public long nextSeq(final String jobId) {
    try (TransactionContext tx = createTransactionContext()) {
      Long max = tx.dsl()
          .select(org.jooq.impl.DSL.max(SEARCH_INDEX_JOB_EVENT.SEQ))
          .from(SEARCH_INDEX_JOB_EVENT)
          .where(SEARCH_INDEX_JOB_EVENT.SEARCH_INDEX_JOB_ID.eq(jobId))
          .fetchOne(0, Long.class);
      return max == null ? 1L : max + 1L;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SEARCH_INDEX_JOB_EVENT;
  }

  @Override
  public Class<SearchIndexJobEvent> getEntityClass() {
    return SearchIndexJobEvent.class;
  }
}

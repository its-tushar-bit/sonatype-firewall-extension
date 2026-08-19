/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJob;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SearchIndexJob.SEARCH_INDEX_JOB;

@Named
@Singleton
public class SearchIndexJobDAO
    extends AbstractOperationalSqlDAO<SearchIndexJob>
{
  @Inject
  public SearchIndexJobDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public Optional<SearchIndexJob> findActiveJob() {
    try (TransactionContext tx = createTransactionContext()) {
      List<SearchIndexJob> jobs = tx.dsl()
          .selectFrom(SEARCH_INDEX_JOB)
          .where(SEARCH_INDEX_JOB.STATUS.in(SearchIndexJob.ACTIVE_STATUSES))
          .orderBy(SEARCH_INDEX_JOB.STARTED_AT.desc().nullsLast(), SEARCH_INDEX_JOB.CREATED_AT.desc())
          .limit(1)
          .fetchInto(SearchIndexJob.class);
      return jobs.stream().findFirst();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SEARCH_INDEX_JOB;
  }

  @Override
  public Class<SearchIndexJob> getEntityClass() {
    return SearchIndexJob.class;
  }
}

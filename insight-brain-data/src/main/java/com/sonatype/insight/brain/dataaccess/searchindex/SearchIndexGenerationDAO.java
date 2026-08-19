/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.searchindex.SearchIndexGeneration;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SearchIndexGeneration.SEARCH_INDEX_GENERATION;

@Named
@Singleton
public class SearchIndexGenerationDAO
    extends AbstractOperationalSqlDAO<SearchIndexGeneration>
{
  @Inject
  public SearchIndexGenerationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Role uniqueness for SERVING/BUILDING is enforced in this layer rather than by a constraint, so order
   * by recency to return the newest claimant deterministically if two rows ever share a role.
   */
  public Optional<SearchIndexGeneration> findByRole(final String role) {
    try (TransactionContext tx = createTransactionContext()) {
      SearchIndexGeneration generation = tx.dsl()
          .selectFrom(SEARCH_INDEX_GENERATION)
          .where(SEARCH_INDEX_GENERATION.ROLE.eq(role))
          .orderBy(SEARCH_INDEX_GENERATION.SERVING_SINCE.desc().nullsLast(),
              SEARCH_INDEX_GENERATION.CREATED_AT.desc())
          .limit(1)
          .fetchOneInto(SearchIndexGeneration.class);
      return Optional.ofNullable(generation);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SEARCH_INDEX_GENERATION;
  }

  @Override
  public Class<SearchIndexGeneration> getEntityClass() {
    return SearchIndexGeneration.class;
  }
}

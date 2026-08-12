/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.searchindex.SearchIndexEstateSnapshot;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SearchIndexEstateSnapshot.SEARCH_INDEX_ESTATE_SNAPSHOT;

@Named
@Singleton
public class SearchIndexEstateSnapshotDAO
    extends AbstractOperationalSqlDAO<SearchIndexEstateSnapshot>
{
  @Inject
  public SearchIndexEstateSnapshotDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Reads the singleton estate snapshot. The row is seeded with zeros and stays that way until the
   * estate analysis that refreshes it lands (CLM-44542), so a zero application count here means "not
   * measured yet" rather than "no applications".
   */
  public SearchIndexEstateSnapshot getCurrent() {
    return getById(SearchIndexEstateSnapshot.CURRENT_ID);
  }

  @Override
  public Table<?> getJooqTable() {
    return SEARCH_INDEX_ESTATE_SNAPSHOT;
  }

  @Override
  public Class<SearchIndexEstateSnapshot> getEntityClass() {
    return SearchIndexEstateSnapshot.class;
  }
}

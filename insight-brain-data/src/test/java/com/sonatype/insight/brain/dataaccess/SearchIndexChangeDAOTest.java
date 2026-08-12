/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexHealthDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SearchIndexChangeDAOTest
    extends AbstractDbDAOTest
{
  private SearchIndexChangeDAO dao;

  private SearchIndexHealthDAO healthDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSearchIndexChangeDAO();
    healthDAO = new SearchIndexHealthDAO(databaseRule.getOperationalDataStore());
    // The first test may initialize the schema which inserts the root org search index change
    // this will be cleared by TemporaryEntity.after for later tests
    List<SearchIndexChange> searchIndexChanges = dao.getAll();
    if (!searchIndexChanges.isEmpty()) {
      assertThat(searchIndexChanges).hasSize(1);
      SearchIndexChange searchIndexChange = searchIndexChanges.get(0);
      assertThat(searchIndexChange).isNotNull();
      assertThat(searchIndexChange.getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
      assertThat(searchIndexChange.getChangeData()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
      dao.delete(searchIndexChange);
    }
    resetHealthCounters();
  }

  @Test
  public void testUpdate() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> dao.update(new SearchIndexChange(ChangeType.APPLICATION, "appId")));
  }

  @Test
  public void testInsert_AdvancedSearchConfigurationEnabled_AdvancedSearchDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(false);

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void testInsert_AdvancedSearchConfigurationEnabled_AdvancedSearchEnabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);

    SearchIndexChange change = new SearchIndexChange(ChangeType.APPLICATION, "appId");
    dao.insert(change);
    assertThat(dao.getAll()).hasSize(1);
    assertThat(dao.getAll().get(0).getChangeType()).isEqualTo(change.getChangeType());
    assertThat(dao.getAll().get(0).getChangeData()).isEqualTo(change.getChangeData());
    assertThat(dao.getAll().get(0).getStatus()).isEqualTo(SearchIndexChange.STATUS_PENDING);
    assertThat(dao.getAll().get(0).getCreatedAt()).isNotNull();
    assertThat(dao.countPending()).isEqualTo(1L);
  }

  /**
   * Queue depth is counted out of this table on demand. Bumping a counter on the single
   * search_index_health CURRENT row instead would put every writer in the product behind one row
   * lock held for the length of the caller's transaction.
   */
  @Test
  public void testInsert_LeavesTheSharedHealthRowAlone() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);
    Date before = healthDAO.getCurrent().getUpdatedAt();

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));

    assertThat(healthDAO.getCurrent().getUpdatedAt()).isEqualTo(before);
    assertThat(healthDAO.getCurrent().getPendingChangeCount()).isZero();
    assertThat(dao.countPending()).isEqualTo(1L);
  }

  @Test
  public void testInsert_AdvancedSearchConfigurationDisabled_AdvancedSearchDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(false);

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void testInsert_AdvancedSearchConfigurationDisabled_AdvancedSearchEnabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void testGetBatch() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);

    for (int i = 0; i < 10; i++) {
      dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    }
    assertThat(dao.getBatch(5)).hasSize(5);
    assertThat(dao.getBatch(11)).hasSize(10);
    assertThat(dao.getAll()).hasSize(10);
    assertThat(dao.countPending()).isEqualTo(10L);
  }

  private void resetHealthCounters() {
    SearchIndexHealth health = healthDAO.getCurrent();
    if (health == null) {
      return;
    }
    health.setPendingChangeCount(0);
    health.setFailedChangeCount(0);
    health.setFailedChangeWindowStart(null);
    health.setOldestPendingCreatedAt(null);
    health.setUpdatedAt(new Date());
    healthDAO.update(health);
  }
}

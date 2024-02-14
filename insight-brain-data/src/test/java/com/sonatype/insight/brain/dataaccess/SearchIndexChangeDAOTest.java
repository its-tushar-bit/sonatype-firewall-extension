/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collections;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SearchIndexChangeDAOTest
    extends AbstractDbDAOTest
{
  private SearchIndexChangeDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSearchIndexChangeDAO();
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
    dao.getAll().forEach(dao::delete);
    assertThat(dao.getAll()).isEmpty();

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void testInsert_AdvancedSearchConfigurationEnabled_AdvancedSearchEnabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);
    dao.getAll().forEach(dao::delete);
    assertThat(dao.getAll()).isEmpty();

    SearchIndexChange change = new SearchIndexChange(ChangeType.APPLICATION, "appId");
    dao.insert(change);
    assertThat(dao.getAll()).usingRecursiveComparison().isEqualTo(Collections.singletonList(change));
  }

  @Test
  public void testInsert_AdvancedSearchConfigurationDisabled_AdvancedSearchDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(false);
    dao.getAll().forEach(dao::delete);
    assertThat(dao.getAll()).isEmpty();

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void testInsert_AdvancedSearchConfigurationDisabled_AdvancedSearchEnabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);
    dao.getAll().forEach(dao::delete);
    assertThat(dao.getAll()).isEmpty();

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    assertThat(dao.getAll()).isEmpty();
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Arrays;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SearchIndexChangeDAOTest
    extends AbstractDbDAOTest
{
  private SearchIndexChangeDAO dao = new SearchIndexChangeDAO();

  @Test
  public void testUpdate() {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
      dao.update(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    });
  }

  @Test
  public void testInsert_AdvancedSearchDisabled() {
    new SystemConfigurationPropertyDAO()
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "false"));

    dao.insert(new SearchIndexChange(ChangeType.APPLICATION, "appId"));
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void testInsert_AdvancedSearchEnabled() {
    new SystemConfigurationPropertyDAO()
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));

    SearchIndexChange change = new SearchIndexChange(ChangeType.APPLICATION, "appId");
    dao.insert(change);
    assertThat(dao.getAll()).usingRecursiveComparison().isEqualTo(Arrays.asList(change));
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FullTextSearchServiceTest
    extends AbstractComponentTest
{
  private final SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @Inject
  private FullTextSearchService fullTextSearchService;

  @Test
  public void testSetStatus_EnableSearch() {
    FullTextSearchStatusDTO fullTextSearchStatusDTO = new FullTextSearchStatusDTO();
    fullTextSearchStatusDTO.isEnabled = true;
    fullTextSearchService.setStatus(fullTextSearchStatusDTO);
    assertThat(isFullTextSearchEnabled()).isTrue();
  }

  @Test
  public void testSetStatus_DisabledSearch() {
    // Given Full Text Search is in enabled state..
    dao.update(new SystemConfigurationProperty(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED, "true"));

    FullTextSearchStatusDTO fullTextSearchStatusDTO = new FullTextSearchStatusDTO();
    fullTextSearchStatusDTO.isEnabled = false;

    fullTextSearchService.setStatus(fullTextSearchStatusDTO);
    assertThat(isFullTextSearchEnabled()).isFalse();
  }

  @Test
  public void testGetStatus_SearchDisabled() {
    FullTextSearchStatusDTO status = fullTextSearchService.getStatus();
    assertThat(status.isEnabled).isFalse();
  }

  @Test
  public void testGetStatus_SearchEnabled() {
    // Given Full Text Search is in enabled state..
    dao.update(new SystemConfigurationProperty(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED, "true"));
    FullTextSearchStatusDTO status = fullTextSearchService.getStatus();
    assertThat(status.isEnabled).isTrue();
  }

  private boolean isFullTextSearchEnabled() {
    return Boolean.parseBoolean(dao.getByName(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED).getValue());
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.File;
import java.util.Arrays;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FullTextSearchServiceTest
    extends AbstractComponentTest
{
  private final SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();

  @Inject
  private FullTextSearchService fullTextSearchService;

  @Inject
  private IndexService indexService;

  @Inject
  private InsightWork insightWork;

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

  @Test
  public void testGetStatus_NoIndex_NullLastIndexTime() {
    assertThat(fullTextSearchService.getStatus().lastIndexTime).isNull();
    assertThat(insightWork.getSearchIndexDir()).doesNotExist();
  }

  @Test
  public void testGetStatus_Index_HasLastIndexTime() throws Exception {
    indexService.createSearchIndex();
    File segmentFile = Arrays.stream(insightWork.getSearchIndexDir().listFiles())
        .filter(file -> file.getName().startsWith("segment")).findFirst().get();
    long firstIndexTime = segmentFile.lastModified();
    assertThat(fullTextSearchService.getStatus().lastIndexTime).isEqualTo(firstIndexTime);
    indexService.createSearchIndex();
    segmentFile = Arrays.stream(insightWork.getSearchIndexDir().listFiles())
        .filter(file -> file.getName().startsWith("segment")).findFirst().get();
    segmentFile.setLastModified(segmentFile.lastModified() + 1000); // Ensure the next index time is different
    long secondIndexTime = segmentFile.lastModified();
    assertThat(secondIndexTime).isGreaterThan(firstIndexTime);
    assertThat(fullTextSearchService.getStatus().lastIndexTime).isEqualTo(secondIndexTime);
  }

  private boolean isFullTextSearchEnabled() {
    return Boolean.parseBoolean(dao.getByName(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED).getValue());
  }
}

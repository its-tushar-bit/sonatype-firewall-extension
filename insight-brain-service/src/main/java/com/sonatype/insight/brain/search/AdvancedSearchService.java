/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AdvancedSearchService
{
  private static final Logger log = LoggerFactory.getLogger(AdvancedSearchService.class);

  private final InsightWork insightWork;

  private final LuceneComponents luceneComponents;

  private final IndexService indexService;

  @Inject
  public AdvancedSearchService(
      InsightWork insightWork,
      LuceneComponents luceneComponents,
      IndexService indexService)
  {
    this.insightWork = insightWork;
    this.luceneComponents = luceneComponents;
    this.indexService = indexService;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setStatus(AdvancedSearchStatusDTO statusDTO) {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    AuditData.get().setData("advancedSearch", statusDTO.isEnabled ? "enabled" : "disabled");
    log.info("Opting {} Advanced Search.", statusDTO.isEnabled ? "in to" : "out of");

    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(statusDTO.isEnabled);
  }

  public AdvancedSearchStatusDTO getStatus() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    AdvancedSearchStatusDTO dto = new AdvancedSearchStatusDTO();
    dto.isEnabled = SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled();
    dto.lastIndexTime = getLastIndexTime();
    dto.isFullIndexTriggered = indexService.isFullIndexTriggered();
    return dto;
  }

  private Long getLastIndexTime() {
    try (Directory directory = luceneComponents.openSearchIndex(true)) {
      if (directory == null) {
        return null;
      }
      String lastCommitSegmentsFileName = SegmentInfos.getLastCommitSegmentsFileName(directory);
      if (lastCommitSegmentsFileName == null) {
        return null;
      }
      return new File(insightWork.getSearchIndexDir(), lastCommitSegmentsFileName).lastModified();
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }
}

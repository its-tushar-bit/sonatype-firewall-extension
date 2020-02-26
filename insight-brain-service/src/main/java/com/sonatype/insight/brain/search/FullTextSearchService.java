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
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class FullTextSearchService
{
  private static final Logger log = LoggerFactory.getLogger(FullTextSearchService.class);

  private final SystemConfigurationPropertyDAO dao;

  private final InsightWork insightWork;

  @Inject
  public FullTextSearchService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO, InsightWork insightWork) {
    this.dao = systemConfigurationPropertyDAO;
    this.insightWork = insightWork;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setStatus(FullTextSearchStatusDTO fullTextSearchStatusDTO) {
    AuditData.get().setData("fullTextSearch", fullTextSearchStatusDTO.isEnabled ? "enabled" : "disabled");
    log.info("Opting {} experimental Full Text Search.", fullTextSearchStatusDTO.isEnabled ? "in to" : "out of");

    String status = Boolean.toString(fullTextSearchStatusDTO.isEnabled);
    dao.update(new SystemConfigurationProperty(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED, status));
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public FullTextSearchStatusDTO getStatus() {
    FullTextSearchStatusDTO dto = new FullTextSearchStatusDTO();
    dto.isEnabled =
        Boolean.parseBoolean(dao.getByName(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED).getValue());
    dto.lastIndexTime = getLastIndexTime();
    return dto;
  }

  private Long getLastIndexTime() {
    File searchIndexDirectory = insightWork.getSearchIndexDir();
    if (!searchIndexDirectory.exists()) {
      return null;
    }
    try (FSDirectory fsDirectory = FSDirectory.open(searchIndexDirectory.toPath())) {
      String lastCommitSegmentsFileName = SegmentInfos.getLastCommitSegmentsFileName(fsDirectory);
      if (lastCommitSegmentsFileName == null) {
        return null;
      }
      return new File(searchIndexDirectory, lastCommitSegmentsFileName).lastModified();
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }
}

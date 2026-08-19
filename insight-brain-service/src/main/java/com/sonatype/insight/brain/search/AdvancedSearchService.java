/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.security.Authorize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AdvancedSearchService
{
  private static final Logger log = LoggerFactory.getLogger(AdvancedSearchService.class);

  private final IndexService indexService;

  @Inject
  public AdvancedSearchService(final IndexService indexService) {
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
    dto.lastIndexTime = indexService.getLastIndexTime();
    dto.isFullIndexTriggered = indexService.isFullIndexTriggered();
    return dto;
  }
}

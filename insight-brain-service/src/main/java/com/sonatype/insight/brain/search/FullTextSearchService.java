/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class FullTextSearchService
{
  private static final Logger log = LoggerFactory.getLogger(FullTextSearchService.class);

  private final SystemConfigurationPropertyDAO dao;

  @Inject
  public FullTextSearchService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.dao = systemConfigurationPropertyDAO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setStatus(FullTextSearchStatusDTO fullTextSearchStatusDTO) {
    log.info("Opting {} experimental Full Text Search.", fullTextSearchStatusDTO.isEnabled ? "in to" : "out of");

    String status = Boolean.toString(fullTextSearchStatusDTO.isEnabled);
    dao.update(new SystemConfigurationProperty(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED, status));
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public FullTextSearchStatusDTO getStatus() {
    FullTextSearchStatusDTO dto = new FullTextSearchStatusDTO();
    dto.isEnabled =
        Boolean.parseBoolean(dao.getByName(SystemConfigurationProperty.FULL_TEXT_SEARCH_ENABLED).getValue());
    return dto;
  }
}

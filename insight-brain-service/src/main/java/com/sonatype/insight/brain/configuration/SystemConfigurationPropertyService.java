/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

/**
 * @since 1.33
 */
@Named
public class SystemConfigurationPropertyService
{
  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public SystemConfigurationPropertyService(SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  public SystemConfigurationProperty getByName(String name) {
    return systemConfigurationPropertyDAO.getByNameNotNull(name);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public SystemConfigurationProperty update(SystemConfigurationProperty property) {
    systemConfigurationPropertyDAO.update(property);
    return property;
  }
}

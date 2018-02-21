/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang.StringUtils;

/**
 * @since 1.43
 */
@Named
public class AutomaticApplicationsConfigurationService
{
  private final OrganizationDAO organizationDAO;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public AutomaticApplicationsConfigurationService(OrganizationDAO organizationDAO,
                                                   SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.organizationDAO = organizationDAO;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION)
  AutomaticApplicationsConfiguration update(AutomaticApplicationsConfiguration configuration) {
    String parentOrganizationId = StringUtils.trimToEmpty(configuration.getParentOrganizationId());
    if (Organization.ROOT_ORGANIZATION_ID.equals(parentOrganizationId)) {
      throw new BadRequestException("Parent cannot be the root organization.");
    }
    if (configuration.isEnabled() && StringUtils.isBlank(parentOrganizationId)) {
      throw new BadRequestException(
          "Parent organization ID is required when automatic application creation is enabled.");
    }
    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      tx.begin();
      if (StringUtils.isNotBlank(parentOrganizationId) && organizationDAO.getById(tx, parentOrganizationId) == null) {
        throw new BadRequestException("Parent organization ID " + parentOrganizationId + " not found.");
      }
      systemConfigurationPropertyDAO.update(tx,
          new SystemConfigurationProperty(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED,
              Boolean.toString(configuration.isEnabled())));
      systemConfigurationPropertyDAO.update(tx,
          new SystemConfigurationProperty(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID,
              parentOrganizationId));
      tx.commit();
    }
    return configuration;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION)
  AutomaticApplicationsConfiguration get() {
    SystemConfigurationProperty enabled = systemConfigurationPropertyDAO
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED);
    SystemConfigurationProperty organizationId = systemConfigurationPropertyDAO
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID);
    return new AutomaticApplicationsConfiguration(Boolean.parseBoolean(enabled.getValue()), organizationId.getValue());
  }
}

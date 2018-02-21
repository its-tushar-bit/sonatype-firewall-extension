/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;
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

  private final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Inject
  public AutomaticApplicationsConfigurationService(OrganizationDAO organizationDAO,
                                                   AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO)
  {
    this.organizationDAO = organizationDAO;
    this.automaticApplicationsConfigurationDAO = automaticApplicationsConfigurationDAO;
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
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      if (StringUtils.isNotBlank(parentOrganizationId) && organizationDAO.getById(tx, parentOrganizationId) == null) {
        throw new BadRequestException("Parent organization ID " + parentOrganizationId + " not found.");
      }
      automaticApplicationsConfigurationDAO.setEnabled(tx, configuration.isEnabled());
      automaticApplicationsConfigurationDAO.setOrganizationId(tx, parentOrganizationId);
      tx.commit();
    }
    return configuration;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION)
  AutomaticApplicationsConfiguration get() {
    return new AutomaticApplicationsConfiguration(automaticApplicationsConfigurationDAO.isEnabled(),
        automaticApplicationsConfigurationDAO.getOrganizationId());
  }
}

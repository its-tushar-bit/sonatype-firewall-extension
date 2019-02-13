/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.18.0
 */
@Named
@Singleton
public class RootOrganizationConfigMigrationService
{
  private static final Logger log = LoggerFactory.getLogger(RootOrganizationConfigMigrationService.class);

  private OrganizationDAO organizationDAO;

  private final RootOrganizationConfigMigrationUtils migrationUtils;

  @Inject
  public RootOrganizationConfigMigrationService(OrganizationDAO organizationDAO,
                                                RootOrganizationConfigMigrationUtils migrationUtils)
  {
    this.organizationDAO = organizationDAO;
    this.migrationUtils = migrationUtils;
  }

  @Authorize(permission = Permission.WRITE)
  public synchronized void setRootOrganizationTemplate(String organizationId) throws IOException {
    Organization organization = organizationDAO.getByIdNotNull(organizationId);

    log.info("Setting template for root organization to {} with id {}", organization.getName(), organizationId);

    if (!isEligibleForRootMigration()) {
      throw new BadRequestException("Migration has previously been scheduled or performed.");
    }
    migrationUtils.setSourceOrganizationId(organizationId);
  }

  @Authorize(permission = Permission.WRITE)
  public synchronized void setRootOrganizationEmptyTemplate() throws IOException {
    log.info("Using empty root organization");

    if (!isEligibleForRootMigration()) {
      throw new BadRequestException("Migration has previously been scheduled or performed.");
    }

    migrationUtils.setMigrated();
  }

  private boolean isEligibleForRootMigration() {
    return !migrationUtils.isMigrated() && !migrationUtils.isMigrationScheduled();
  }
}

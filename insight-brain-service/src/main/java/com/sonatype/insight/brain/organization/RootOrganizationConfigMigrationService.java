/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;

import org.codehaus.plexus.util.FileUtils;
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

  public static final String MIGRATE_FILE = "rootorganization-migration";

  public static final String MIGRATED_FILE = "rootorganization-migrated";

  private OrganizationDAO organizationDAO;

  private InsightWork insightWork;

  @Inject
  public RootOrganizationConfigMigrationService(OrganizationDAO organizationDAO, InsightWork insightWork) {
    this.organizationDAO = organizationDAO;
    this.insightWork = insightWork;
  }

  @Authorize(permission = Permission.WRITE)
  public synchronized void setRootOrganizationTemplate(String organizationId) throws IOException {
    Organization organization = organizationDAO.getByIdNotNull(organizationId);

    log.info("Setting template for root organization to {} with id {}", organization.getName(), organizationId);

    if (!isEligibleForRootMigration()) {
      throw new BadRequestException("Migration has previously been scheduled or performed.");
    }
    FileUtils.fileWrite(getMigrateFile(), organization.getId());
  }

  @Authorize(permission = Permission.WRITE)
  public synchronized void setRootOrganizationEmptyTemplate() throws IOException {
    log.info("Using empty root organization");

    if (!isEligibleForRootMigration()) {
      throw new BadRequestException("Migration has previously been scheduled or performed.");
    }

    FileUtils.fileWrite(getMigratedFile(), "");
  }

  private boolean isEligibleForRootMigration() {
    return !isMigrated() && !isMigrationScheduled();
  }

  public boolean isMigrated() {
    return getMigratedFile().isFile();
  }

  public boolean isMigrationScheduled() {
    return getMigrateFile().isFile();
  }

  private File getMigrateFile() {
    return new File(insightWork.getWorkDir(), MIGRATE_FILE);
  }

  private File getMigratedFile() {
    return new File(insightWork.getWorkDir(), MIGRATED_FILE);
  }
}

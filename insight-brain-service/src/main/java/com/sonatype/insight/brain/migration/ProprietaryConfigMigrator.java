/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.dataaccess.ObsoleteProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightWork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates proprietary.json to the db
 *
 * @since 1.22
 */
@Named
public class ProprietaryConfigMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ProprietaryConfigMigrator.class);

  static final String MARKER_FILE_NAME = "proprietaryconfig-migrated";

  private final InsightWork work;

  private final ObsoleteProprietaryConfigDAO obsoleteProprietaryConfigDAO;

  private final ProprietaryConfigDAO proprietaryConfigDAO;

  @Inject
  public ProprietaryConfigMigrator(InsightWork work, ProprietaryConfigDAO proprietaryConfigDAO) {
    this.work = work;
    this.obsoleteProprietaryConfigDAO = new ObsoleteProprietaryConfigDAO(work.getDataDir());
    this.proprietaryConfigDAO = proprietaryConfigDAO;
  }

  void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Migrating proprietary config data...");

    File markerFile = new File(work.getWorkDir(), MARKER_FILE_NAME);
    if (markerFile.exists()) {
      log.info("Proprietary config already migrated.");
      return;
    }

    com.sonatype.clm.dto.model.ProprietaryConfig obsoleteConfig = obsoleteProprietaryConfigDAO.get();

    if (obsoleteConfig.getRegexes().isEmpty() && obsoleteConfig.getPackages().isEmpty()) {
      markerFile.createNewFile();
      return;
    }

    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(obsoleteConfig.getPackages());
    proprietaryConfig.setRegexes(obsoleteConfig.getRegexes());
    proprietaryConfig.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    proprietaryConfigDAO.insert(proprietaryConfig);

    markerFile.createNewFile();

    log.info("Migrated proprietary config data in {} ms.", System.currentTimeMillis() - start);
  }
}

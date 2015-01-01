/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.13.0
 */
public abstract class AbstractAuditGAVMigrator
{
  private static final Logger log = LoggerFactory.getLogger(AbstractAuditGAVMigrator.class);

  private final InsightWork insightWork;

  public AbstractAuditGAVMigrator(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  public int migrate() throws IOException {
    int migratedFileCount = 0;

    File baseAuditDir = insightWork.getAuditDir("");
    log.debug("Base audit directory {}", baseAuditDir.getAbsolutePath());
    File markerFile = new File(baseAuditDir, getMarkerFilename());

    if (!baseAuditDir.exists()) {
      // Nothing to migrate
      log.info("Base audit directory {} does not exist yet.", baseAuditDir.getAbsolutePath());
      baseAuditDir.mkdirs();
    }
    else if (markerFile.exists()) {
      log.debug("Marker file already exists: {}", getMarkerFilename());
    }
    else {
      File[] auditDirs = baseAuditDir.listFiles();
      if (auditDirs == null || auditDirs.length == 0) {
        log.info("Found no audit data.", baseAuditDir.getAbsolutePath());
        markerFile.createNewFile();
        return migratedFileCount;
      }

      ApplicationDAO applicationDAO = new ApplicationDAO();
      OrganizationDAO organizationDAO = new OrganizationDAO();

      for (File auditDir : auditDirs) {
        if (!auditDir.isDirectory()) {
          continue;
        }

        String ownerId = auditDir.getName();
        Application application = applicationDAO.getById(ownerId);
        if (application == null) {
          Organization organization = organizationDAO.getById(ownerId);
          if (organization == null) {
            log.info("Cannot find an organization or application with id {}. It was probably deleted.", ownerId);
            continue;
          }
        }

        File auditJsonFile = new File(auditDir, getAuditFileName());
        if (!auditJsonFile.exists()) {
          log.info("Cannot find a {} file for ownerId: {}.", getAuditFileName(), ownerId);
          continue;
        }
        log.info("Migrating audit {} file {} for ownerId {}", getAuditFileName(), auditJsonFile.getAbsolutePath(),
            ownerId);
        replaceGAVs(auditJsonFile);
        migratedFileCount++;
      }
    }

    markerFile.createNewFile();
    return migratedFileCount;
  }

  /**
   * Update the structure to move GAV into a ComponentIdentifier and write the result over the existing file.
   */
  private void replaceGAVs(final File jsonAuditFile) throws IOException {
    ArrayNode jsonAuditData = JsonUtils.read(jsonAuditFile);
    for (int x = 0; x < jsonAuditData.size(); x++) {
      ContainerNode<?> data = (ContainerNode<?>) jsonAuditData.get(x);
      if (data != null && data.has("data")) // stamped data?
      {
        data = (ContainerNode<?>) data.get("data");
      }
      if (data instanceof ArrayNode) {
        for (int y = 0; y < data.size(); y++) {
          ComponentIdentifierAdapter.replaceGavWithComponentIdentifier((ObjectNode) data.get(y));
        }
      }
      else {
        ComponentIdentifierAdapter.replaceGavWithComponentIdentifier((ObjectNode) data);
      }
    }
    JsonUtils.write(jsonAuditFile, jsonAuditData);
  }

  /**
   * The name of the audit file to be migrated.
   */
  protected abstract String getAuditFileName();

  /**
   * The file that denotes whether or not the migration step has run.
   */
  protected abstract String getMarkerFilename();
}

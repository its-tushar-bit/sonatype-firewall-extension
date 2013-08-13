/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates the license overrides from the audit log to the ODS database.
 * 
 * @since 1.6
 */
@Named
public class LicenseOverrideMigrator
{
  private static final Logger log = LoggerFactory.getLogger(LicenseOverrideMigrator.class);

  private final InsightWork insightWork;

  static final String MARKER_FILE_NAME = "licenseoverrides-migrated";

  @Inject
  public LicenseOverrideMigrator(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Migrating license override data...");

    File baseAuditDir = insightWork.getAuditDir("");
    log.debug("Base audit directory {}", baseAuditDir.getAbsolutePath());
    File markerFile = new File(baseAuditDir, MARKER_FILE_NAME);
    if (!baseAuditDir.exists()) {
      // Nothing to migrate
      log.info("Base audit directory {} does not exist yet.", baseAuditDir.getAbsolutePath());
      baseAuditDir.mkdirs();
      markerFile.createNewFile();
      return;
    }

    if (markerFile.exists()) {
      log.info("License overrides already migrated.");
      return;
    }

    File[] auditDirs = baseAuditDir.listFiles();
    if (auditDirs == null || auditDirs.length == 0) {
      log.info("Found no audit data.", baseAuditDir.getAbsolutePath());
      markerFile.createNewFile();
      return;
    }

    int applicationCount = 0;
    ApplicationDAO applicationDAO = new ApplicationDAO();
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    EntityManager em = applicationDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      for (File auditDir : auditDirs) {
        if (!auditDir.isDirectory()) {
          continue;
        }

        String applicationId = auditDir.getName();
        Application application = applicationDAO.getById(em, applicationId);
        if (application == null) {
          log.info("Cannot find an application with id {}. It was probably deleted.", applicationId);
          continue;
        }

        File licenseJsonFile = new File(auditDir, "licenses.json");
        if (!licenseJsonFile.exists()) {
          log.info("Cannot find a license json file for application {} (id {}).", application.getName(), applicationId);
          continue;
        }

        applicationCount++;

        ArrayNode licenseJsonData = (ArrayNode) JsonUtils.read(licenseJsonFile);
        // Aggregate all the changes found in the licenseJsonData log into one flat list
        List<JsonNode> licenseAuditChanges = new ArrayList<JsonNode>();
        for (int x = 0; x < licenseJsonData.size(); x++) {
          ContainerNode<?> data = (ContainerNode<?>) licenseJsonData.get(x);
          if (data != null && data.has("data")) // stamped data?
          {
            data = (ContainerNode<?>) data.get("data");
          }
          if (data instanceof ArrayNode) {
            for (int y = 0; y < data.size(); y++) {
              licenseAuditChanges.add(data.get(y));
            }
          }
          else {
            licenseAuditChanges.add(data);
          }
        }

        // Process the license audit changes and create license overrides in the db
        Set<String> seenGavs = new LinkedHashSet<String>();
        for (JsonNode licenseAuditChange : licenseAuditChanges) {
          String groupId = licenseAuditChange.get("groupId").asText();
          String artifactId = licenseAuditChange.get("artifactId").asText();
          String version = licenseAuditChange.get("version").asText();
          String gav = groupId + ":" + artifactId + ":" + version;
          if (seenGavs.contains(gav)) {
            // We already found a license override for this gav - this must be an old audit record.
            continue;
          }
          seenGavs.add(gav);

          String statusName = licenseAuditChange.get("status").asText();

          String licenseOverrideId = null;
          LicenseOverrideStatus status = LicenseOverrideStatus.getByName(statusName);
          JsonNode licenseOverrideJsonNode = licenseAuditChange.get("overriddenLicenses");
          if (licenseOverrideJsonNode != null) {
            licenseOverrideJsonNode = licenseOverrideJsonNode.get(0);
            if (licenseOverrideJsonNode != null) {
              String licenseOverrideName = licenseOverrideJsonNode.asText();
              licenseOverrideId = new LicenseDAO().getByNameNotNull(licenseOverrideName).getId();
            }
          }
          String comment = JsonUtils.getNullableString(licenseAuditChange.get("comment"));
          LicenseOverride licenseOverride = new LicenseOverride(applicationId, groupId, artifactId, version, status,
              licenseOverrideId, comment);
          licenseOverrideDAO.insert(licenseOverride);
        }
        log.info("Migrated {} license overrides for application {} (id {}).", seenGavs.size(), application.getName(),
            applicationId);
      }

      em.getTransaction().commit();

      markerFile.createNewFile();
    }
    finally {
      AbstractDAO.close(em);
    }

    log.info("Migrated license override data for {} applications in {} ms.", applicationCount,
        System.currentTimeMillis() - start);
  }
}

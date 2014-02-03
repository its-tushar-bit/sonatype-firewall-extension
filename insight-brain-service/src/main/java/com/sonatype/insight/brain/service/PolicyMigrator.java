/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.H2DatabaseMigrator;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates the policies from the file system to the ODS database.
 * 
 * @since 1.9
 */
@Named
public class PolicyMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyMigrator.class);

  private final InsightWork insightWork;

  static final String MARKER_FILE_NAME = "policies-migrated";

  @Inject
  public PolicyMigrator(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Migrating policy data...");

    File markerFile = new File(insightWork.getWorkDir(), MARKER_FILE_NAME);
    if (markerFile.exists()) {
      log.debug("Policies already migrated.");
      return;
    }

    int ownerCount = 0;

    File basePolicyDir = new File(insightWork.getWorkDir(), "policy");
    log.debug("Base policy directory {}", basePolicyDir.getAbsolutePath());
    if (!basePolicyDir.exists()) {
      // Nothing to migrate
      log.debug("Base policy directory {} does not exist yet.", basePolicyDir.getAbsolutePath());
    }
    else {
      File[] policyDirs = basePolicyDir.listFiles();
      if (policyDirs == null || policyDirs.length == 0) {
        log.info("Found no policy data.", basePolicyDir.getAbsolutePath());
      }
      else {
        OrganizationDAO orgDAO = new OrganizationDAO();
        ApplicationDAO appDAO = new ApplicationDAO();
        PolicyDAO policyDAO = new PolicyDAO();
        EntityManager em = orgDAO.createEntityManager();
        try {
          em.getTransaction().begin();

          for (File policyDir : policyDirs) {
            if (!policyDir.isDirectory()) {
              continue;
            }

            String ownerId = policyDir.getName();
            String ownerName;
            Application app = appDAO.getById(em, ownerId);
            if (app == null) {
              Organization org = orgDAO.getById(em, ownerId);
              if (org == null) {
                log.info("Cannot find an application or organization with id {}. It was probably deleted.", ownerId);
                continue;
              }
              else {
                ownerName = org.getName();
              }
            }
            else {
              ownerName = app.getName();
            }

            File policiesFile = new File(policyDir, "policy.json");
            if (!policiesFile.exists()) {
              log.info("Cannot find a policy.json file for application or organization {} (id {}).", ownerName, ownerId);
              continue;
            }

            ownerCount++;

            JsonStore jsonStore = JsonUtils.fileStore(policyDir);
            ArrayNode policyJsonData = (ArrayNode) jsonStore.restore("policy.json");
            for (JsonNode policyJsonNode : policyJsonData) {
              Policy policy = JsonUtils.asPojo(policyJsonNode, Policy.class);
              policy.setOwnerId(ownerId);
              policyDAO.insert(em, policy);
            }
            log.info("Migrated {} policies for application or organization {} (id {}).", policyJsonData.size(),
                ownerName, ownerId);
          }

          em.getTransaction().commit();
        }
        finally {
          AbstractDAO.close(em);
        }
      }
    }

    addForeignKeys();

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();

    log.info("Migrated policy data for {} applications or organizations in {} ms.", ownerCount,
        System.currentTimeMillis() - start);
  }

  private void addForeignKeys() {
    try {
      String scriptName = "/db/" + OperationalDataStoreProvider.ID + "/schema_incremental_0040a.sql";
      new H2DatabaseMigrator().runScript(OperationalDataStoreProvider.getDataSource(), scriptName);
    }
    catch (Exception e) {
      Throwable cause = e;
      while (cause != null) {
        // NOTE: The exception message is localized
        if (cause.getMessage() != null && cause.getMessage().contains("\"policy_waiver_policy_fk\"")) {
          return;
        }
        cause = cause.getCause();
      }
      throw new RuntimeException(e);
    }
  }
}

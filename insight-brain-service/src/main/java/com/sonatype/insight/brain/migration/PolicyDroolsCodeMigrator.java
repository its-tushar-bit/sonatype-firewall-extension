/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.SchemaInfoDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.H2DatabaseMigrator;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.SchemaInfo;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.brain.service.InsightWork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Drools code for all policies.
 * 
 * @since 1.12
 */
@Named
public class PolicyDroolsCodeMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyDroolsCodeMigrator.class);

  private final InsightWork insightWork;

  private final SchemaInfoDAO schemaInfoDAO;

  // v2 since 1.16
  // v3 since 1.32
  static final int DROOLS_CODE_VERSION = 3;

  static final String MARKER_FILE_NAME = "policy-drools-code-migrated";

  @Inject
  public PolicyDroolsCodeMigrator(InsightWork insightWork, SchemaInfoDAO schemaInfoDAO) {
    this.insightWork = insightWork;
    this.schemaInfoDAO = schemaInfoDAO;
  }

  void migrate() {
    File markerFile = new File(insightWork.getWorkDir(), MARKER_FILE_NAME);

    SchemaInfo schemaInfo = schemaInfoDAO.get();
    int droolsCodeVersion = schemaInfo.getDroolsCodeVersion();
    if (droolsCodeVersion <= 0) {
      droolsCodeVersion = markerFile.exists() ? 1 : 0;
    }

    if (droolsCodeVersion >= DROOLS_CODE_VERSION) {
      log.debug("Policy code already generated.");
      return;
    }

    long start = System.currentTimeMillis();
    log.debug("Generating policy code...");

    PolicyDAO policyDAO = new PolicyDAO();
    List<Policy> policies = policyDAO.getAll();
    log.info("Found {} policies.", policies.size());
    for (Policy policy : policies) {
      DroolsGenerator.generate(policy);
      // NOTE: Due to CLM-8176, we skip validation and focus on just updating the Drools code
      policyDAO.update(policy, false);
    }

    schemaInfo.setDroolsCodeVersion(DROOLS_CODE_VERSION);
    schemaInfoDAO.update(schemaInfo);

    if (droolsCodeVersion < 1) {
      alterDroolsCodeColumnToNotAllowNulls();
    }

    if (droolsCodeVersion < 2 && !markerFile.delete()) {
      log.debug("Failed to delete obsolete marker file {}", markerFile);
    }

    log.info("Generated policy code for {} policies in {} ms.", policies.size(), System.currentTimeMillis() - start);
  }

  private void alterDroolsCodeColumnToNotAllowNulls() {
    String scriptName = "/db/" + OperationalDataStoreProvider.ID + "/schema_incremental_0057a.sql";
    try {
      new H2DatabaseMigrator().runScript(OperationalDataStoreProvider.getDataSource(), scriptName);
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to set policy.drools_code to not allow nulls", e);
    }
  }
}

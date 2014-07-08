/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.H2DatabaseMigrator;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.DroolsGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Drools code for all policies.
 * 
 * @since 1.12
 */
@Named
public class PolicyCodeMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyCodeMigrator.class);

  private final InsightWork insightWork;

  static final String MARKER_FILE_NAME = "policies-code-migrated";

  @Inject
  public PolicyCodeMigrator(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Generating policy code...");

    File markerFile = new File(insightWork.getWorkDir(), MARKER_FILE_NAME);
    if (markerFile.exists()) {
      log.debug("Policy code already generated.");
      return;
    }

    PolicyDAO policyDAO = new PolicyDAO();
    List<Policy> policies = policyDAO.getAll();
    log.info("Found {} policies.", policies.size());
    for (Policy policy : policies) {
      DroolsGenerator.generate(policy);
      policyDAO.update(policy);
    }

    alterDroolsCodeColumnToNotAllowNulls();

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();

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

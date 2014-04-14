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

import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes references to procure stage from policy actions and policy monitoring.
 * 
 * @since 1.11
 */
@Named
public class ProcureRemovalMigrator
{
  public static final String ID_PROCURE = "procure";

  private static final Logger log = LoggerFactory.getLogger(ProcureRemovalMigrator.class);

  static final String MARKER_FILE_NAME = "procure-removal";

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  private final InsightWork insightWork;

  @Inject
  public ProcureRemovalMigrator(InsightWork insightWork)
  {
    this.insightWork = insightWork;
  }

  void migrate() throws IOException {
    File markerFile = new File(insightWork.getWorkDir(), MARKER_FILE_NAME);

    if (markerFile.exists()) {
      return;
    }
    
    //moved policy action migration to PolicyMigrator, the actions need
    //to be pruned prior to being placed in the database
    
    migratePolicyMonitors();

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();
  }

  private void migratePolicyMonitors() {
    long start = System.currentTimeMillis();
    log.info("Removing procure monitors");

    for (PolicyMonitoring monitor : policyMonitoringDAO.getAll()) {
      if (ID_PROCURE.equals(monitor.getStageTypeId())) {
        policyMonitoringDAO.delete(monitor);
      }
    }
    log.info("Finished procure policy monitoring removal in {} ms.", System.currentTimeMillis() - start);
  }
}

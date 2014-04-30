/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.model.HasStringId;

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

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final OrganizationDAO orgDAO = new OrganizationDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

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
      log.debug("References to procure stage already migrated.");
      return;
    }
    migratePolicyActions();
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

  private void migratePolicyActions() {
    long start = System.currentTimeMillis();
    log.info("Removing policy procure actions...");

    for (Application app : appDAO.getAll()) {
      log.debug("Checking application {}", app.getName());
      migrate(app);
    }

    for (Organization org : orgDAO.getAll()) {
      log.debug("Checking organization {}", org.getName());
      migrate(org);
    }

    log.info("Finished procure policy actions removal in {} ms.", System.currentTimeMillis() - start);
  }

  private void migrate(HasStringId context) {
    for (Policy policy : policyDAO.getByOwnerId(context.getId())) {
      if (pruneProcurement(policy)) {
        policyDAO.update(policy); 
      }
    }
  }
  
  public boolean pruneProcurement( Policy policy) {
    log.debug("Checking policy {}", policy.getName());
    Map<String, List<Action>> actions = policy.getActions();
    if (actions != null && actions.containsKey(ID_PROCURE)) {
      log.debug("Removing procure action");
      actions.remove(ID_PROCURE);
      return true;
    }
    return false;
  }
}

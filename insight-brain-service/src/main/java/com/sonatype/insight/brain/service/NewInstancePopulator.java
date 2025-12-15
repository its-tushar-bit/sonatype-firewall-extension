/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.policy.PolicyImportExport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class checks to see if this is a fresh instance of IQ, and if so, runs various routines that
 * are meant to install default or sample data
 */
@Named
public class NewInstancePopulator
{
  private static final Logger log = LoggerFactory.getLogger(NewInstancePopulator.class);

  private final ReferencePolicyFetcher referencePolicyFetcher;

  private final SampleDataCreator sampleDataCreator;

  private final PolicyImportExport policyImportExport;

  private final InsightConfig insightConfig;

  private final AuditRecorder auditRecorder;

  private final ClusterLockManager clusterLockManager;

  private final OrganizationDAO organizationDAO;

  private final PolicyDAO policyDAO;

  @Inject
  public NewInstancePopulator(
      final ReferencePolicyFetcher referencePolicyFetcher,
      final SampleDataCreator sampleDataCreator,
      final PolicyImportExport policyImportExport,
      final InsightConfig insightConfig,
      final AuditRecorder auditRecorder,
      final ClusterLockManager clusterLockManager,
      final OrganizationDAO organizationDAO,
      final PolicyDAO policyDAO)
  {
    this.referencePolicyFetcher = referencePolicyFetcher;
    this.sampleDataCreator = sampleDataCreator;
    this.policyImportExport = policyImportExport;
    this.insightConfig = insightConfig;
    this.auditRecorder = auditRecorder;
    this.clusterLockManager = clusterLockManager;
    this.organizationDAO = organizationDAO;
    this.policyDAO = policyDAO;
  }

  void populateIfNewInstance() {
    try (ClusterLock clusterLock = clusterLockManager.createForNewInstancePopulation()) {
      clusterLock.lock();
      doPopulateIfNewInstance();
    }
  }

  // Visible for testing
  void doPopulateIfNewInstance() {
    long start = System.currentTimeMillis();

    List<Organization> orgs = organizationDAO.getAll();
    List<Policy> policies = policyDAO.getAll();

    // create sample data only for new installs
    if (policies.isEmpty() && orgs.size() == 1
        && orgs.get(0).getId().equalsIgnoreCase(Organization.ROOT_ORGANIZATION_ID)) {
      populate(orgs.get(0));
    }

    log.debug("populateIfNewInstance finished in {} ms.", System.currentTimeMillis() - start);
  }

  private void populate(Organization rootOrganization) {
    boolean createSampleData = insightConfig.isCreateSampleData();

    if (insightConfig.isImportReferencePoliciesFromHDS()) {
      log.info("Importing Reference Policies");

      try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.IMPORT)) {
        AuditData.get().setOrganization(rootOrganization);
        try {
          policyImportExport.importOrganizationWithoutAuthorizationCheck(rootOrganization,
              referencePolicyFetcher.getReferencePolicies());
        }
        catch (Exception e) {
          log.error("Unable to import Reference Policies from HDS", e);
          AuditData.get().setException(e);
          // skip the sample data creation or we will not re-attempt to import the policies/ltgs on a later restart
          createSampleData = false;
        }
        catch (Throwable t) {
          AuditData.get().setException(t);
          throw t;
        }
      }
    }

    if (createSampleData) {
      log.info("Creating Sample Data");
      sampleDataCreator.createSampleData();
    }
  }
}

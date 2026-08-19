/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.43
 */
@Named
public class SampleDataCreator
{
  public static final String SAMPLE_ORGANIZATION_NAME = "Sandbox Organization";

  public static final String SAMPLE_APPLICATION_NAME = "Sandbox Application";

  public static final String SAMPLE_APPLICATION_PUBLIC_ID = "sandbox-application";

  private final AuditRecorder auditRecorder;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  @Inject
  public SampleDataCreator(
      final AuditRecorder auditRecorder,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO)
  {
    this.auditRecorder = auditRecorder;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
  }

  public void createSampleData() {
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      Organization sampleOrganization = new Organization(SAMPLE_ORGANIZATION_NAME);
      organizationDAO.insert(tx, sampleOrganization);
      Application sampleApplication = new Application(SAMPLE_APPLICATION_PUBLIC_ID, SAMPLE_APPLICATION_NAME,
          sampleOrganization.getId());
      applicationDAO.insert(tx, sampleApplication);
      tx.commit();
      auditSampleOrganization(sampleOrganization);
      auditSampleApplication(sampleOrganization, sampleApplication);
    }
  }

  private void auditSampleOrganization(final Organization organization) {
    try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.CREATE_ORGANIZATION)) {
      AuditData.get().setOrganization(organization);
    }
  }

  private void auditSampleApplication(final Organization organization, final Application application) {
    try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.CREATE_APPLICATION)) {
      AuditData.get().setParentOrganization(organization).setApplicationWithDetails(application);
    }
  }
}

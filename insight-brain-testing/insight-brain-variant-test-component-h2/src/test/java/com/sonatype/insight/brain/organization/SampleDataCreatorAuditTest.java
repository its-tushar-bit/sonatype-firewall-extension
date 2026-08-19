/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuditTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

@ComponentH2Test
public class SampleDataCreatorAuditTest
    extends AbstractComponentH2AuditTest
{
  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private SampleDataCreator sampleDataCreator;

  @Test
  public void testCreateSampleData() {
    sampleDataCreator.createSampleData();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null, SYSTEM_USER);
    Organization organization = organizationDAO.getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    assertOrganizationData(auditDTO, organization);

    auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null, SYSTEM_USER);
    Application application = applicationDAO.getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    assertApplicationData(auditDTO, application);
    assertParentOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "contactUsername", application.getContactInternalName());
  }
}

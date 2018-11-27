/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Test;

public class SampleDataCreatorAuditTest
    extends AbstractAuditTest
{
  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  @After
  public void cleanup() {
    Application app = applicationDAO.getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    Organization org = organizationDAO.getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    if (app != null) {
      applicationDAO.delete(app);
    }
    if (org != null) {
      organizationDAO.delete(org);
    }
  }

  @Test
  public void testCreateSampleData() {
    getCLMServer().getInjector().getInstance(SampleDataCreator.class).createSampleData();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null, SYSTEM_USER);
    Organization organization = organizationDAO.getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    assertOrganizationData(auditDTO, organization);

    auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null, SYSTEM_USER);
    Application application = applicationDAO.getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    assertApplicationData(auditDTO, application);
    assertParentOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "contactUsername", application.getContactInternalName());
  }

  private void assertParentOrganizationData(final AuditDTO auditDTO, final Organization parentOrganization) {
    assertCustomData(auditDTO, "parentOrganizationId", parentOrganization.getId());
    assertCustomData(auditDTO, "parentOrganizationName", parentOrganization.getName());
  }
}

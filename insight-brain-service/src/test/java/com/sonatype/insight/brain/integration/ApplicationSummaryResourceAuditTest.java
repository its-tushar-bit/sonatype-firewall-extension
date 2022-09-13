/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApplicationSummaryResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testVerifyOrCreateApplication() throws Exception {
    Organization organization = tempEntity.newOrganizationAutomaticApplicationsConfiguration();
    String nonExistentAppPublicId = tempEntity.uuid();

    verifyOrCreateApplicationRequest().parameter(nonExistentAppPublicId).post();
    Application persistedApp = new ApplicationDAO().getByPublicIdNotNull(nonExistentAppPublicId);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.AUTO_CREATE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, persistedApp, organization);
  }

  private void assertDetailedApplicationData(final AuditDTO auditDTO,
                                             final Application application,
                                             final Organization organization)
  {
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", application.getContactInternalName());
    assertParentOrganizationData(auditDTO, organization);
  }

  private HttpRequest verifyOrCreateApplicationRequest() {
    return restRequest().path(DefaultApplicationSummaryResource.RESOURCE_PATH)
        .path(DefaultApplicationSummaryResource.VERIFY_OR_CREATE_APPLICATION_PATH)
        .query("goal", Goal.EVALUATE_APPLICATION);
  }
}

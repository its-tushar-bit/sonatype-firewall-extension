/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.license.LicenseThreatGroupLicenseResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqPostgresTest
class IqPostgresLicenseThreatGroupLicenseResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void tearDown() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private HttpRequest restRequest(Owner owner, String ltgId) {
    return ctx.restRequest()
        .path(LicenseThreatGroupLicenseResource.RESOURCE_PATH)
        .parameter(owner.getType(),
            owner.getPublicId(), ltgId);
  }

  private void assertLicenseData(AuditDTO auditDTO, LicenseThreatGroup ltg, String... licenseNames) {
    assertCustomData(auditDTO, "licenseThreatGroupId", ltg.getId());
    assertCustomData(auditDTO, "licenseThreatGroupName", ltg.getName());
    assertCustomData(auditDTO, "licenseNames", Arrays.asList(licenseNames));
  }

  @Test
  void testSetLicenseThreatGroupLicenses_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(application.getId());
    restRequest(application, ltg.getId()).body(Collections.emptyList()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, null);
    assertApplicationData(auditDTO, application);
    assertLicenseData(auditDTO, ltg);
  }

  @Test
  void testSetLicenseThreatGroupLicenses_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(organization.getId());
    restRequest(organization, ltg.getId()).body(Arrays.asList("Apache-UNSPECIFIED", "PUBLIC-DOMAIN")).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, null);
    assertOrganizationData(auditDTO, organization);
    assertLicenseData(auditDTO, ltg, "Apache", "Public Domain");
  }

  @Test
  void testSetLicenseThreatGroupLicenses_Unauthorized() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    LicenseThreatGroup ltg = ctx.tempEntity().newLicenseThreatGroup(organization.getId());
    restRequest(organization, ltg.getId())
        .with(httpRequest -> httpRequest.auth(unauthorizedUser))
        .body(Collections.singletonList("Not-Declared"))
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... names) {
      super(names);
    }

    void tearDown() {
      after();
    }
  }
}

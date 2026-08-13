/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.licensing.product.ProductLicenseManager;

/**
 * Converted from the legacy {@code ApiProductLicenseResourceAuditTest}.
 */
@IqH2Test
class IqH2ApiProductLicenseResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void after() {
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

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH);
  }

  private HttpRequest licenseRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH)
        .part("file", "sonatype.lic", new byte[1]);
  }

  private void uploadLicense(HttpRequest licenseRequest) throws Exception {
    licenseRequest.post();
  }

  private TestProductLicenseManager getTestProductLicenseManager() {
    return (TestProductLicenseManager) ctx.lookup(ProductLicenseManager.class);
  }

  @Test
  void testInstallLicense() throws Exception {
    ctx.installLicense();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INSTALL_LICENSE, null);
    assertLicenseData(auditDTO, "sonatype.lic");
  }

  @Test
  void testInstallLicense_Unauthorized() throws Exception {
    uploadLicense(licenseRequest().with(unauthorizedUser()));

    assertAuditLog(AuditEvent.INSTALL_LICENSE, "unauthorized");
  }

  @Test
  void testUninstallLicense() throws Exception {
    ctx.uninstallLicense();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UNINSTALL_LICENSE, null);
    assertLicenseData(auditDTO, null);
  }

  @Test
  void testUninstallLicense_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.UNINSTALL_LICENSE, "unauthorized");
  }

  private void assertLicenseData(AuditDTO auditDTO, String filename) {
    assertCustomData(auditDTO, "productLicenseFingerprint", "1234");
    assertCustomData(auditDTO, "productLicenseFilename", filename);
    String productLicenseExpiry = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(getTestProductLicenseManager().getExpirationDate().getTime()),
            ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
    assertCustomData(auditDTO, "productLicenseExpiry", productLicenseExpiry);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}

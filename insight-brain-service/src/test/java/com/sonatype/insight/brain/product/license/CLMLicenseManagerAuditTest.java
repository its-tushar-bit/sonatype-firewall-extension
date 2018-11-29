/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static org.junit.Assert.fail;

public class CLMLicenseManagerAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testInstallLicenseIfUnlicensed() throws Exception {
    uninstallLicense();
    File licenseFile = tempDir.newFile();

    getCLMServer().getInjector().getInstance(CLMLicenseManager.class)
        .installLicenseIfUnlicensed(licenseFile.getAbsolutePath());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INSTALL_LICENSE, null, SYSTEM_USER);
    assertLicenseData(auditDTO, licenseFile.getName());
  }

  @Test
  public void testInstallLicenseIfUnlicensed_ServerError() throws Exception {
    uninstallLicense();

    try {
      getCLMServer().getInjector().getInstance(CLMLicenseManager.class).installLicenseIfUnlicensed("doesNotExist");
      fail("Expected exception");
    }
    catch (Throwable t) {
      assertAuditLog(AuditEvent.INSTALL_LICENSE, "server-error", SYSTEM_USER);
    }
  }

  private void assertLicenseData(AuditDTO auditDTO, String filename) {
    assertCustomData(auditDTO, "productLicenseFingerprint", "1234");
    assertCustomData(auditDTO, "productLicenseFilename", filename);
    String productLicenseExpiry = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(getTestProductLicenseManager().getExpirationDate().getTime()),
            ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
    assertCustomData(auditDTO, "productLicenseExpiry", productLicenseExpiry);
  }
}

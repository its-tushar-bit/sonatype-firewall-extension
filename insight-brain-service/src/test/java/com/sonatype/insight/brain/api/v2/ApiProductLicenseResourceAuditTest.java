/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiProductLicenseResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testInstallLicense() throws Exception {
    installLicense();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INSTALL_LICENSE, null);
    assertLicenseData(auditDTO, "sonatype.lic");
  }

  @Test
  public void testInstallLicense_Unauthorized() throws Exception {
    uploadLicense(licenseRequest().with(unauthorizedUser()));

    assertAuditLog(AuditEvent.INSTALL_LICENSE, "unauthorized");
  }

  @Test
  public void testUninstallLicense() throws Exception {
    uninstallLicense();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UNINSTALL_LICENSE, null);
    assertLicenseData(auditDTO, null);
  }

  @Test
  public void testUninstallLicense_Unauthorized() throws Exception {
    restRequest().path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH).with(unauthorizedUser()).delete();

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
}

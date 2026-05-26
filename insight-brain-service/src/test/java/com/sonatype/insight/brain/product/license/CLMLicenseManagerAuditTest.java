/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentAuditTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;

public class CLMLicenseManagerAuditTest
    extends AbstractComponentAuditTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private CLMLicenseManager licenseManager;

  @Inject
  private TestProductLicenseManager testProductLicenseManager;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Before
  public void before() {
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @Test
  public void testInstallLicenseIfUnlicensed() throws Exception {
    licenseManager.uninstallLicense();
    File licenseFile = tempDir.newFile();
    Files.write(licenseFile.toPath(), new byte[1]);

    licenseManager.installLicenseIfUnlicensed(licenseFile.getAbsolutePath());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INSTALL_LICENSE, null, SYSTEM_USER);
    assertLicenseData(auditDTO, licenseFile.getName());
  }

  @Test
  public void testInstallLicenseIfUnlicensed_ServerError() {
    licenseManager.uninstallLicense();

    assertThatExceptionOfType(Exception.class).isThrownBy(
        () -> licenseManager.installLicenseIfUnlicensed("doesNotExist"));
    assertAuditLog(AuditEvent.INSTALL_LICENSE, "server-error", SYSTEM_USER);
  }

  private void assertLicenseData(AuditDTO auditDTO, String filename) {
    assertCustomData(auditDTO, "productLicenseFingerprint", "1234");
    assertCustomData(auditDTO, "productLicenseFilename", filename);
    String productLicenseExpiry = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(testProductLicenseManager.getExpirationDate().getTime()),
            ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
    assertCustomData(auditDTO, "productLicenseExpiry", productLicenseExpiry);
  }
}

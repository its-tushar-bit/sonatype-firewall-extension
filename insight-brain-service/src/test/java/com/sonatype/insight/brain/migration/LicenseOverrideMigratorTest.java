/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.DummyLicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @since 1.6
 */
public class LicenseOverrideMigratorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private static LicenseDataUpdater savedLicenseDataUpdater;

  @BeforeClass
  public static void beforeClass() {
    savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();
    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());
  }

  @AfterClass
  public static void afterClass() {
    LicenseDataUpdater.setUpdater(savedLicenseDataUpdater);
  }

  @Test
  public void testMigrate() throws Exception {
    Application application = tempEntity.newApplicationWithParent("LicenseOverrideMigratorTestAppId");

    InsightWork insightWork = createInsightWork();
    File auditDir = insightWork.getAuditDir("");
    File appAuditDir = new File(auditDir, application.getId());
    appAuditDir.mkdirs();
    assertTrue(appAuditDir.isDirectory());
    URL testAuditFileUrl = getClass().getResource("/LicenseOverrideMigratorTest/licenses.json");
    FileUtils.copyFile(new File(testAuditFileUrl.getFile()), new File(appAuditDir, "licenses.json"));

    LicenseOverrideMigrator migrator = new LicenseOverrideMigrator(insightWork);
    migrator.migrate();

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    assertThat(licenseOverrideDAO.getByOwnerId(application.getId()), hasSize(4));

    assertLicenseOverride(application.getId(), "org.slf4j", "slf4j-api", "1.7.2", LicenseOverrideStatus.OVERRIDDEN,
        "Apache-2.0", "Comment 1");
    assertLicenseOverride(application.getId(), "tomcat", "tomcat-util", "5.5.23", LicenseOverrideStatus.ACKNOWLEDGED,
        null /* licenseId */, "Comment 3");
    assertLicenseOverride(application.getId(), "commons-dbcp", "commons-dbcp", "1.4", LicenseOverrideStatus.OVERRIDDEN,
        "GPL-3.0", "Comment 4");
    assertLicenseOverride(application.getId(), "commons-pool", "commons-pool", "1.4", LicenseOverrideStatus.OVERRIDDEN,
        "GPL-3.0", "Comment 4");
  }

  @Test
  public void testMigrate_UnknownLicense() throws Exception {
    Application application = tempEntity.newApplicationWithParent("LicenseOverrideMigratorTestAppId");

    InsightWork insightWork = createInsightWork();
    File auditDir = insightWork.getAuditDir("");
    File appAuditDir = new File(auditDir, application.getId());
    appAuditDir.mkdirs();
    assertTrue(appAuditDir.isDirectory());
    URL testAuditFileUrl = getClass().getResource("/LicenseOverrideMigratorTest/licenses_unknown_license.json");
    FileUtils.copyFile(new File(testAuditFileUrl.getFile()), new File(appAuditDir, "licenses.json"));

    LicenseOverrideMigrator migrator = new LicenseOverrideMigrator(insightWork);
    migrator.migrate();

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    assertThat(licenseOverrideDAO.getByOwnerId(application.getId()), hasSize(1));

    assertLicenseOverride(application.getId(), "commons-pool", "commons-pool", "1.4", LicenseOverrideStatus.OVERRIDDEN,
        "GPL-3.0", "Comment 4");
  }

  private void assertLicenseOverride(String ownerId,
                                     String groupId,
                                     String artifactId,
                                     String version,
                                     LicenseOverrideStatus status,
                                     String licenseId,
                                     String comment)
  {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    LicenseOverride actual = new LicenseOverrideDAO().getByOwnerIdAndComponentIdentifier(ownerId, componentIdentifier);
    assertNotNull(actual);
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(componentIdentifier, actual.getComponentIdentifier());
    assertEquals(status, actual.getStatus());
    assertEquals(licenseId != null ? Collections.singleton(licenseId) : Collections.emptySet(), actual.getLicenseIds());
    assertEquals(comment, actual.getComment());
  }

  @Test
  public void testMigrate_AuditDirDoesNotExist() throws Exception {
    InsightWork insightWork = createInsightWork();
    File auditDir = insightWork.getAuditDir("");
    assertFalse(auditDir.exists());
    File markerFile = new File(insightWork.getAuditDir(""), LicenseOverrideMigrator.MARKER_FILE_NAME);

    LicenseOverrideMigrator migrator = new LicenseOverrideMigrator(insightWork);
    migrator.migrate();

    assertTrue(markerFile.exists());
  }

  @Test
  public void testMigrate_AuditDirEmpty() throws Exception {
    InsightWork insightWork = createInsightWork();
    File auditDir = insightWork.getAuditDir("");
    auditDir.mkdirs();
    assertTrue(auditDir.exists());
    assertEquals(0, auditDir.listFiles().length);
    File markerFile = new File(auditDir, LicenseOverrideMigrator.MARKER_FILE_NAME);
    assertFalse(markerFile.exists());

    LicenseOverrideMigrator migrator = new LicenseOverrideMigrator(insightWork);
    migrator.migrate();

    assertTrue(markerFile.exists());
  }

  @Test
  public void testMigrate_ApplicationDoesNotExist() throws Exception {
    InsightWork insightWork = createInsightWork();
    File auditDir = insightWork.getAuditDir("");
    File appAuditDir = new File(auditDir, "YetiId");
    appAuditDir.mkdirs();
    assertTrue(appAuditDir.isDirectory());
    File markerFile = new File(auditDir, LicenseOverrideMigrator.MARKER_FILE_NAME);
    assertFalse(markerFile.exists());

    LicenseOverrideMigrator migrator = new LicenseOverrideMigrator(insightWork);
    migrator.migrate();

    assertTrue(markerFile.exists());
  }

  @Test
  public void testMigrate_ExcessiveComment() throws Exception {
    Application application = tempEntity.newApplicationWithParent("LicenseOverrideMigratorTestAppId");

    InsightWork insightWork = createInsightWork();
    File auditDir = insightWork.getAuditDir("");
    File appAuditDir = new File(auditDir, application.getId());
    appAuditDir.mkdirs();
    assertTrue(appAuditDir.isDirectory());
    FileUtils.copyURLToFile(getClass().getResource("/LicenseOverrideMigratorTest/licenses_excessive_comment.json"),
        new File(appAuditDir, "licenses.json"));

    LicenseOverrideMigrator migrator = new LicenseOverrideMigrator(insightWork);
    migrator.migrate();

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    assertThat(licenseOverrideDAO.getByOwnerId(application.getId()), hasSize(1));

    assertLicenseOverride(application.getId(), "commons-pool", "commons-pool", "1.4", LicenseOverrideStatus.OVERRIDDEN,
        "GPL-3.0", StringUtils.repeat("123456789_", 100));
  }

  private InsightWork createInsightWork() throws IOException {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    InsightWork work = new InsightWork(insightConfig);
    return work;
  }
}

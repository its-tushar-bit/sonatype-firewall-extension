/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @since 1.27
 */
@H2DiskTest
@Category(SlowTest.class)
public class SupportServiceTest
    extends AbstractComponentTest
{
  private static final String CONFIG_YML_FILENAME = "config-support-test.yml";

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private Configuration configuration;

  @Inject
  private SupportService supportService;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private InsightWork insightWork;

  private File originalConfigFile;

  @Before
  public void before() {
    originalConfigFile = ApplicationLifecycle.getConfigFile();
  }

  @After
  public void after() {
    ApplicationLifecycle.setConfigFile(originalConfigFile);
  }

  private File getConfigYml() {
    return new File(getClass().getResource("/" + getClass().getSimpleName() + "/" + CONFIG_YML_FILENAME).getFile());
  }

  @Test
  public void testCreateSupportZip() throws Exception {
    assertThat(supportService.createSupportZip(false, null, false)).isNotNull();
  }

  @Test
  public void testCreateSupportZipFilenameRules() throws Exception {
    final String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    final String nowPrefix = now.substring(0, now.indexOf("-"));

    final File firstZip = supportService.createSupportZip(false, null, false);
    final String firstFilename = firstZip.getName();
    assertThat(firstFilename).startsWith("support-" + nowPrefix);
    final int zipIndex = firstFilename.indexOf(".zip");
    final int lastDash = firstFilename.lastIndexOf('-');
    final int counterValue = Integer.parseInt(firstFilename.substring(lastDash + 1, zipIndex));

    final File secondZip = supportService.createSupportZip(false, null, false);
    assertThat(secondZip.getName()).startsWith("support-" + nowPrefix).endsWith("-" + (counterValue + 1) + ".zip");
  }

  @Test
  public void testCreateSupportZip_UsesSubDir() throws Exception {
    supportService.createSupportZip(false, null, false);
    assertThat(supportService.getWorkDir()).isDirectory();
  }

  @Test
  public void testCreateSupportZip_DeletesFilteredFile() throws Exception {
    final File configYml = getConfigYml();
    ApplicationLifecycle.setConfigFile(configYml);
    supportService.createSupportZip(false, null, false);
    final File filteredConfigYml = new File(supportService.getWorkDir(), "filtered-" + configYml.getName());
    assertThat(filteredConfigYml.exists()).isFalse();
  }

  private static void verifyRequiredEntries(final File supportZipFile, final Enumeration<? extends ZipEntry> entries) {
    final String zipFileBasename = getZipFileBasename(supportZipFile);

    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/sysinfo.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/product-version.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/product-license.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/threads.txt");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/jmx.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/ldap.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/proxy-server.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/saml.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/mail.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/crowd.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/oauth2Configuration.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/oidcConfiguration.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/dbFileInfo.txt");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/config.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/scm.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/systemConfigurationProperties.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/featuresConfigurationProperties.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/migrationTracker.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/systemConfiguration.json");
    assertThat(entries.nextElement().getName()).isEqualTo(
        zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/dataRetentionPolicy.json");
  }

  private static String getZipFileBasename(final File supportZipFile) {
    return supportZipFile.getName().substring(0, supportZipFile.getName().length() - 4);
  }

  @Test
  public void testCreateSupportZip_NoConfigFile() throws Exception {
    final File configYml = new File("config-I-dont-exist.yml");
    ApplicationLifecycle.setConfigFile(configYml);
    final File supportZip = supportService.createSupportZip(false, null, false);
    // read file from zip and assert no config file entry
    try (final ZipFile zipFile = new ZipFile(supportZip)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      verifyRequiredEntries(supportZip, entries);
      assertThat(entries.hasMoreElements()).isFalse();
    }
  }

  @Test
  public void testCreateSupportZip_HasRequiredEntries() throws Exception {
    ApplicationLifecycle.setConfigFile(getConfigYml());
    final File supportZip = supportService.createSupportZip(false, null, false);
    try (final ZipFile zipFile = new ZipFile(supportZip)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      assertThat(entries.nextElement().getName()).isEqualTo(getZipFileBasename(supportZip) + "/"
          + SupportFileType.CONFIG.getDirName() + "/filtered-" + CONFIG_YML_FILENAME);
      verifyRequiredEntries(supportZip, entries);
      assertThat(entries.hasMoreElements()).isFalse();
    }
  }

  @Test
  public void testCreateSupportZip_HasEntryProductVersionSorted() throws Exception {
    ApplicationLifecycle.setConfigFile(getConfigYml());
    final File supportZip = supportService.createSupportZip(false, null, false);
    try (final ZipFile zipFile = new ZipFile(supportZip)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      assertThat(entries.nextElement().getName()).isEqualTo(getZipFileBasename(supportZip) + "/"
          + SupportFileType.CONFIG.getDirName() + "/filtered-" + CONFIG_YML_FILENAME);
      assertThat(entries.nextElement().getName())
          .isEqualTo(getZipFileBasename(supportZip) + "/" + SupportFileType.INFO.getDirName() + "/sysinfo.json");

      final ZipEntry zipEntry = entries.nextElement();
      assertThat(zipEntry.getName()).isEqualTo(
          getZipFileBasename(supportZip) + "/" + SupportFileType.INFO.getDirName() + "/product-version.json");
      try (final ByteArrayOutputStream zipEntryContent = new ByteArrayOutputStream()) {
        try (final InputStream zipEntryStream = zipFile.getInputStream(zipEntry)) {
          IOUtils.copy(zipEntryStream, zipEntryContent);
          final JsonNode result = JsonUtils.parse(zipEntryContent.toString("UTF-8"));
          assertThat(result.size()).isEqualTo(1);
          final JsonNode parentNode = result.get("product-version");
          final Iterator<String> children = parentNode.fieldNames();
          assertThat(children.next()).isEqualTo("build");
          assertThat(children.next()).isEqualTo("name");
          assertThat(children.next()).isEqualTo("tag");
          assertThat(children.next()).isEqualTo("timestamp");
          assertThat(children.next()).isEqualTo("version");
          assertThat(parentNode.size()).isEqualTo(5);
        }
      }
    }
  }

  @Test
  public void testAddAllDbData() throws IOException {
    final List<SupportFile> filesToZip = new ArrayList<>();
    final File workDir = tempDir.newFolder("dbDataTest");
    supportService.addAllDbData(filesToZip, workDir);

    final String[] basenames = new String[]{
      "repositoryManager",
      "repository",
      "organization",
      "application",
      "proprietaryConfig",
      "user",
      "role",
      "rolePermission",
      "membershipMapping",
      "webhook",
      "systemNotice",
      "label",
      "componentLabel",
      "tag",
      "applicationTag",
      "policyTag",
      "securityVulnerabilityOverride",
      "licenseThreatGroup",
      "multiLicense",
      "license",
      "licenseThreatGroupLicense",
      "policy",
      "policyMonitoring",
      "sourceControl",
      "reverseProxyAuthenticationConfiguration",
      "innerSourceRepositoryConnection",
      "cpeMatchingConfiguration",
      "ciIntegrationsConfig"
    };
    final File[] expectedFiles = createExpectedFiles(workDir, basenames);
    assertThat(workDir.listFiles()).containsExactlyInAnyOrder(expectedFiles);
  }

  private File[] createExpectedFiles(final File workDir, final String[] basenames) {
    final File[] expectedFiles = new File[basenames.length];
    for (int i = 0; i < basenames.length; i++) {
      expectedFiles[i] = new File(workDir, basenames[i] + ".json");
    }
    return expectedFiles;
  }

  private File createPopulatedZip(final boolean noLimit, final List<SupportFile> filesToAdd) throws Exception {
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, 1L);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES);
    final File workDir = tempDir.newFolder("populateZipTest");
    final String prefix = "prefix";
    final File supportZip = new File(workDir, prefix + ".zip").getCanonicalFile();

    for (SupportFile fileToAdd : filesToAdd) {
      assertThat(fileToAdd.file.length()).isGreaterThan(configuration.getSupportReadLimitBytes());
    }

    supportService.populateZip(prefix, supportZip, filesToAdd, noLimit);
    return supportZip;
  }

  @Test
  public void testPopulateZip_Limit() throws Exception {
    SupportFile logFile = new SupportFile(SupportFileType.LOG, createFile(50), false);
    SupportFile clusterLogFile = new SupportFile(SupportFileType.CLUSTER_LOG, createFile(50), false);
    SupportFile otherFile = new SupportFile(SupportFileType.INFO, getConfigYml(), false);

    File supportZip = createPopulatedZip(false, Arrays.asList(logFile, clusterLogFile, otherFile));

    long expectedTruncatedSize =
        configuration.getSupportReadLimitBytes() + (SupportService.TRUNCATED_TOKEN + "\n").length();
    try (ZipFile zipFile = new ZipFile(supportZip)) {
      List<? extends ZipEntry> entries = Collections.list(zipFile.entries());

      assertThat(entries).map(ZipEntry::getName).contains(getZipFileBasename(supportZip) + "/" + "truncated");

      ZipEntry logFileZipEntry = getZipEntryNotNull(supportZip, entries, logFile);
      assertThat(logFileZipEntry.getSize()).isEqualTo(expectedTruncatedSize).isLessThan(logFile.file.length());
      assertThat(getZipEntryContent(zipFile, logFileZipEntry)).startsWith(SupportService.TRUNCATED_TOKEN);

      ZipEntry clusterLogFileZipEntry = getZipEntryNotNull(supportZip, entries, clusterLogFile);
      assertThat(clusterLogFileZipEntry.getSize()).isEqualTo(expectedTruncatedSize)
          .isLessThan(clusterLogFile.file.length());
      assertThat(getZipEntryContent(zipFile, clusterLogFileZipEntry)).startsWith(SupportService.TRUNCATED_TOKEN);

      ZipEntry otherFileZipEntry = getZipEntryNotNull(supportZip, entries, otherFile);
      assertThat(otherFileZipEntry.getSize()).isEqualTo(otherFile.file.length());
    }
  }

  @Test
  public void testPopulateZip_NoLimit() throws Exception {
    SupportFile logFile = new SupportFile(SupportFileType.LOG, createFile(50), false);
    SupportFile clusterLogFile = new SupportFile(SupportFileType.CLUSTER_LOG, createFile(50), false);
    SupportFile otherFile = new SupportFile(SupportFileType.INFO, getConfigYml(), false);

    File supportZip = createPopulatedZip(true, Arrays.asList(logFile, clusterLogFile, otherFile));

    try (ZipFile zipFile = new ZipFile(supportZip)) {
      List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
      ZipEntry logFileZipEntry = getZipEntryNotNull(supportZip, entries, logFile);
      assertThat(logFileZipEntry.getSize()).isEqualTo(logFile.file.length());
      ZipEntry clusterLogFileZipEntry = getZipEntryNotNull(supportZip, entries, clusterLogFile);
      assertThat(clusterLogFileZipEntry.getSize()).isEqualTo(clusterLogFile.file.length());
      ZipEntry otherFileZipEntry = getZipEntryNotNull(supportZip, entries, otherFile);
      assertThat(otherFileZipEntry.getSize()).isEqualTo(otherFile.file.length());
    }
  }

  @Test
  public void testCreateSupportZip_IncludesClusterLogFiles() throws Exception {
    File clusterDirectory = tempDir.newFolder();
    insightConfig.setClusterDirectory(clusterDirectory.getAbsolutePath());
    File clusterLogFile1 = clusterDirectory.toPath().resolve(Paths.get("log", "a.log")).toFile();
    File clusterLogFile2 = clusterDirectory.toPath().resolve(Paths.get("log", "b.log")).toFile();
    // Should not be included because it doesn't match the filename pattern for log files.
    File otherFile = clusterDirectory.toPath().resolve(Paths.get("log", "other")).toFile();
    // Should not be included because it's older than today.
    File oldFile = clusterDirectory.toPath().resolve(Paths.get("log", "old.log")).toFile();
    FileUtils.writeStringToFile(clusterLogFile1, "a", StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(clusterLogFile2, "b", StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(otherFile, "c", StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(oldFile, "d", StandardCharsets.UTF_8);

    Instant todayStartTime = Instant.now().truncatedTo(ChronoUnit.DAYS);
    clusterLogFile1.setLastModified(todayStartTime.toEpochMilli());
    clusterLogFile2.setLastModified(todayStartTime.toEpochMilli());
    otherFile.setLastModified(todayStartTime.toEpochMilli());
    oldFile.setLastModified(todayStartTime.toEpochMilli() - 1);

    File supportZip = supportService.createSupportZip(false, null, false);

    try (ZipFile zipFile = new ZipFile(supportZip)) {
      List<ZipEntry> entries = EnumerationUtils.toList(zipFile.entries());
      ZipEntry clusterLogEntry1 = entries.stream().filter(e -> e.getName().endsWith("a.log")).findFirst().orElse(null);
      ZipEntry clusterLogEntry2 = entries.stream().filter(e -> e.getName().endsWith("b.log")).findFirst().orElse(null);
      ZipEntry otherEntry = entries.stream().filter(e -> e.getName().endsWith("other")).findFirst().orElse(null);
      ZipEntry oldEntry = entries.stream().filter(e -> e.getName().endsWith("old.log")).findFirst().orElse(null);
      assertThat(clusterLogEntry1).isNotNull();
      assertThat(IOUtils.toString(zipFile.getInputStream(clusterLogEntry1), StandardCharsets.UTF_8)).isEqualTo("a");
      assertThat(clusterLogEntry2).isNotNull();
      assertThat(IOUtils.toString(zipFile.getInputStream(clusterLogEntry2), StandardCharsets.UTF_8)).isEqualTo("b");
      assertThat(otherEntry).isNull();
      assertThat(oldEntry).isNull();
    }
  }

  @Test
  public void testExcludeDirFilter() {
    IOFileFilter dirFilter = supportService.excludeDirFilter();

    assertThat(dirFilter.accept(insightWork.getReportDir())).isFalse();
    assertThat(dirFilter.accept(new File("/safe-dir/clm-cluster/logs/server/file.log"))).isTrue();
  }

  @Test
  public void testExcludeDirFilter_WithNestedDirectories() {
    IOFileFilter dirFilter = supportService.excludeDirFilter();

    assertThat(dirFilter.accept(insightWork.getClusterCacheDir())).isFalse();
    assertThat(dirFilter.accept(new File(insightWork.getClusterCacheDir() + "/test"))).isFalse();
  }

  @Test
  public void testCreateSupportZip_ThrowsExceptionWhenAlreadyInProgress() throws Exception {
    // This test verifies that when a support zip generation is already in progress,
    // a second concurrent request will fail with SupportZipInProgressException.
    // We use two threads to simulate concurrent requests.

    final Thread[] threads = new Thread[2];
    final Exception[] exceptions = new Exception[2];
    final File[] results = new File[2];
    final CountDownLatch firstThreadStarted = new CountDownLatch(1);
    final CountDownLatch secondThreadCanStart = new CountDownLatch(1);

    // First thread: signals when it starts, then waits briefly before creating support zip
    threads[0] = new Thread(() -> {
      try {
        firstThreadStarted.countDown();
        secondThreadCanStart.await();
        results[0] = supportService.createSupportZip(false, null, false);
      }
      catch (Exception e) {
        exceptions[0] = e;
      }
    });

    // Second thread: waits for first thread to start, then creates support zip
    threads[1] = new Thread(() -> {
      try {
        firstThreadStarted.await();
        secondThreadCanStart.countDown();
        results[1] = supportService.createSupportZip(false, null, false);
      }
      catch (Exception e) {
        exceptions[1] = e;
      }
    });

    // Start both threads
    threads[0].start();
    threads[1].start();

    // Wait for both threads to complete
    threads[0].join();
    threads[1].join();

    // One thread should succeed, the other should fail with SupportZipInProgressException
    boolean success = (results[0] != null && exceptions[0] == null) || (results[1] != null && exceptions[1] == null);
    boolean failure = (exceptions[0] instanceof SupportZipInProgressException) ||
        (exceptions[1] instanceof SupportZipInProgressException);

    assertThat(success).isTrue();
    assertThat(failure).isTrue();

    // Verify the exception message
    SupportZipInProgressException exception = null;
    if (exceptions[0] != null && exceptions[0] instanceof SupportZipInProgressException) {
      exception = (SupportZipInProgressException) exceptions[0];
    }
    else if (exceptions[1] != null && exceptions[1] instanceof SupportZipInProgressException) {
      exception = (SupportZipInProgressException) exceptions[1];
    }
    assertThat(exception.getMessage()).contains("Support zip generation is already in progress");
  }

  @Test
  public void testCreateSupportZip_IncludesCpeMatchingConfiguration() throws Exception {
    // Create test data
    Organization org = tempEntity.newOrganization();
    tempEntity.newCpeMatchingConfiguration(org.getId(), true, true);
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newCpeMatchingConfiguration(app.getId(), false, false);

    // Create support zip
    File supportZip = supportService.createSupportZip(true, null, false);
    assertThat(supportZip).exists();

    // Verify CPE configuration file is in the zip
    try (ZipFile zipFile = new ZipFile(supportZip)) {
      List<? extends ZipEntry> entries = EnumerationUtils.toList(zipFile.entries());
      String expectedPath =
          getZipFileBasename(supportZip) + "/" + SupportFileType.DB.getDirName() + "/cpeMatchingConfiguration.json";

      ZipEntry cpeConfigEntry = entries.stream()
          .filter(e -> e.getName().equals(expectedPath))
          .findFirst()
          .orElse(null);

      assertThat(cpeConfigEntry).isNotNull();

      // Verify content
      String content = getZipEntryContent(zipFile, cpeConfigEntry);
      assertThat(content).isNotEmpty();
      JsonNode jsonNode = JsonUtils.parse(content);
      assertThat(jsonNode.has("cpeMatchingConfiguration")).isTrue();
      assertThat(jsonNode.get("cpeMatchingConfiguration").isArray()).isTrue();
      assertThat(jsonNode.get("cpeMatchingConfiguration").size()).isGreaterThanOrEqualTo(2);
    }
  }

  @Test
  public void testCreateSupportZip_IncludesAuthConfigWithoutIncludeDb() throws Exception {
    tempEntity.newOAuth2Configuration();
    tempEntity.newOidcConfiguration("https://idp.example.com", "client-id", "super-secret",
        "https://idp.example.com/auth", "https://idp.example.com/token");
    tempEntity.newCrowdConfiguration();

    File supportZip = supportService.createSupportZip(false, null, false);

    try (ZipFile zipFile = new ZipFile(supportZip)) {
      List<? extends ZipEntry> entries = EnumerationUtils.toList(zipFile.entries());
      String base = getZipFileBasename(supportZip) + "/" + SupportFileType.CONFIG.getDirName() + "/";

      ZipEntry oauth2Entry = entries.stream()
          .filter(e -> e.getName().equals(base + "oauth2Configuration.json"))
          .findFirst()
          .orElse(null);
      assertThat(oauth2Entry).isNotNull();
      assertThat(getZipEntryContent(zipFile, oauth2Entry)).contains("https://an-idp");

      ZipEntry oidcEntry = entries.stream()
          .filter(e -> e.getName().equals(base + "oidcConfiguration.json"))
          .findFirst()
          .orElse(null);
      assertThat(oidcEntry).isNotNull();
      String oidcContent = getZipEntryContent(zipFile, oidcEntry);
      assertThat(oidcContent).contains("client-id");

      ZipEntry crowdEntry = entries.stream()
          .filter(e -> e.getName().equals(base + "crowd.json"))
          .findFirst()
          .orElse(null);
      assertThat(crowdEntry).isNotNull();
      String crowdContent = getZipEntryContent(zipFile, crowdEntry);
      assertThat(crowdContent).contains("http://localhost:8095/crowd");
    }
  }

  private File createFile(int sizeInBytes) throws Exception {
    File file = tempDir.newFile();
    byte[] bytes = new byte[sizeInBytes];
    Arrays.fill(bytes, (byte) 1);
    FileUtils.writeByteArrayToFile(file, bytes);
    return file;
  }

  private ZipEntry getZipEntryNotNull(File supportZip, List<? extends ZipEntry> entries, SupportFile supportFile) {
    ZipEntry entry = entries.stream()
        .filter(e -> e.getName()
            .equals(
                getZipFileBasename(supportZip) + "/" + supportFile.supportFileType.getDirName() + "/" +
                    supportFile.file.getName()))
        .findFirst()
        .orElse(null);
    assertThat(entry).isNotNull();
    return entry;
  }

  private String getZipEntryContent(ZipFile zipFile, ZipEntry zipEntry) throws Exception {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
        IOUtils.copy(inputStream, byteArrayOutputStream);
      }
      return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
    }
  }
}

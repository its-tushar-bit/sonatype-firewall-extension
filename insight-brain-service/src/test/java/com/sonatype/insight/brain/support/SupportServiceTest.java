/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.commons.io.filefilter.IOFileFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.27
 */
@H2DiskTest
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
    originalConfigFile = InsightBrainService.getConfigFile();
  }

  @After
  public void after() {
    InsightBrainService.setConfigFile(originalConfigFile);
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
    final int counterValue = Integer.parseInt(firstFilename.substring(zipIndex - 1, zipIndex));

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
    InsightBrainService.setConfigFile(configYml);
    supportService.createSupportZip(false, null, false);
    final File filteredConfigYml = new File(supportService.getWorkDir(), "filtered-" + configYml.getName());
    assertThat(filteredConfigYml.exists()).isFalse();
  }

  private static void verifyRequiredEntries(final File supportZipFile, final Enumeration<? extends ZipEntry> entries) {
    final String zipFileBasename = getZipFileBasename(supportZipFile);

    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/sysinfo.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/product-version.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/product-license.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/threads.txt");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/jmx.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/ldap.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/proxy-server.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/saml.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/mail.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/dbFileInfo.txt");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/config.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/scm.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/migrationTracker.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/systemConfiguration.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/dataRetentionPolicy.json");
  }

  private static String getZipFileBasename(final File supportZipFile) {
    return supportZipFile.getName().substring(0, supportZipFile.getName().length() - 4);
  }

  @Test
  public void testCreateSupportZip_NoConfigFile() throws Exception {
    final File configYml = new File("config-I-dont-exist.yml");
    InsightBrainService.setConfigFile(configYml);
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
    InsightBrainService.setConfigFile(getConfigYml());
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
    InsightBrainService.setConfigFile(getConfigYml());
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
        "innerSourceRepositoryConnection"
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
    File otherFile = clusterDirectory.toPath().resolve(Paths.get("log", "other")).toFile();
    FileUtils.writeStringToFile(clusterLogFile1, "a", StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(clusterLogFile2, "b", StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(otherFile, "c", StandardCharsets.UTF_8);

    File supportZip = supportService.createSupportZip(false, null, false);

    try (ZipFile zipFile = new ZipFile(supportZip)) {
      List<ZipEntry> entries = EnumerationUtils.toList(zipFile.entries());
      ZipEntry clusterLogEntry1 = entries.stream().filter(e -> e.getName().endsWith("a.log")).findFirst().orElse(null);
      ZipEntry clusterLogEntry2 = entries.stream().filter(e -> e.getName().endsWith("b.log")).findFirst().orElse(null);
      ZipEntry otherEntry = entries.stream().filter(e -> e.getName().endsWith("other")).findFirst().orElse(null);
      assertThat(clusterLogEntry1).isNotNull();
      assertThat(IOUtils.toString(zipFile.getInputStream(clusterLogEntry1), StandardCharsets.UTF_8)).isEqualTo("a");
      assertThat(clusterLogEntry2).isNotNull();
      assertThat(IOUtils.toString(zipFile.getInputStream(clusterLogEntry2), StandardCharsets.UTF_8)).isEqualTo("b");
      assertThat(otherEntry).isNull();
    }
  }

  @Test
  public void testExcludeDirFilter() {
    IOFileFilter dirFilter = supportService.excludeDirFilter();

    Assert.assertFalse(dirFilter.accept(insightWork.getReportDir()));
    Assert.assertTrue(dirFilter.accept(new File("test-work/clm-cluster/logs/server/file.log")));
  }

  @Test
  public void testExcludeDirFilter_WithNestedDirectories() {
    IOFileFilter dirFilter = supportService.excludeDirFilter();

    Assert.assertFalse(dirFilter.accept(insightWork.getClusterCacheDir()));
    Assert.assertFalse(dirFilter.accept(new File(insightWork.getClusterCacheDir() + "/test")));
  }

  private File createFile(int sizeInBytes) throws Exception {
    File file = tempDir.newFile();
    byte[] bytes = new byte[sizeInBytes];
    Arrays.fill(bytes, (byte) 1);
    FileUtils.writeByteArrayToFile(file, bytes);
    return file;
  }

  private ZipEntry getZipEntryNotNull(File supportZip, List<? extends ZipEntry> entries, SupportFile supportFile) {
    ZipEntry entry = entries.stream().filter(e -> e.getName().equals(
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

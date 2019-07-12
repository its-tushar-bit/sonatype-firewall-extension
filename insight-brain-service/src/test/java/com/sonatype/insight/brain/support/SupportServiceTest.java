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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.support.LimitedFileInputStreamTest.CONFIG_YML;
import static com.sonatype.insight.brain.support.LimitedFileInputStreamTest.CONFIG_YML_FILENAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.27
 */
public class SupportServiceTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig insightConfig;

  @Inject
  private SupportService supportService;

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
    return new File(SupportServiceTest.class.getResource(CONFIG_YML).getFile());
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

  @Test
  public void testCreateSupportZip_TruncatedFileStartsWithToken() throws Exception {
    InsightBrainService.setConfigFile(getConfigYml());

    insightConfig.getSupportConfig().setReadLimitBytes(500);

    final File supportZip = supportService.createSupportZip(false, null, false);
    // read file from zip and assert token suffix
    try (final ZipFile zipFile = new ZipFile(supportZip)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      final ZipEntry zipEntry = entries.nextElement();
      assertThat(zipEntry.getName()).isEqualTo(getZipFileBasename(supportZip) + "/"
          + SupportFileType.CONFIG.getDirName() + "/filtered-" + CONFIG_YML_FILENAME);
      try (final ByteArrayOutputStream zipEntryContent = new ByteArrayOutputStream()) {
        try (final InputStream zipEntryStream = zipFile.getInputStream(zipEntry)) {
          IOUtil.copy(zipEntryStream, zipEntryContent);
        }
        assertThat(zipEntryContent.toString("UTF-8")).startsWith(SupportService.TRUNCATED_TOKEN);
      }
    }
  }

  @Test
  public void testCreateSupportZip_TruncatedZipIncludesTruncatedEntry() throws Exception {
    InsightBrainService.setConfigFile(getConfigYml());

    insightConfig.getSupportConfig().setReadLimitBytes(5);

    final File supportZip = supportService.createSupportZip(false, null, false);
    // read zip and assert truncated entry
    try (final ZipFile zipFile = new ZipFile(supportZip)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      assertThat(entries.nextElement().getName()).isEqualTo(getZipFileBasename(supportZip) + "/"
          + SupportFileType.CONFIG.getDirName() + "/filtered-" + CONFIG_YML_FILENAME);
      verifyRequiredEntries(supportZip, entries);
      assertThat(entries.nextElement().getName()).isEqualTo(getZipFileBasename(supportZip) + "/" + "truncated");
    }
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
        .isEqualTo(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/dbFileInfo.txt");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/migrationTracker.json");
    assertThat(entries.nextElement().getName())
        .isEqualTo(zipFileBasename + "/" + SupportFileType.DB.getDirName() + "/systemConfiguration.json");
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
          IOUtil.copy(zipEntryStream, zipEntryContent);
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
        "policyMonitoring"
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

  private File createPopulatedZip(final boolean noLimit, final File fileToAdd) throws Exception {
    insightConfig.getSupportConfig().setReadLimitBytes(1);
    final File workDir = tempDir.newFolder("populateZipTest");
    final String prefix = "prefix";
    final File supportZip = new File(workDir, prefix + ".zip").getCanonicalFile();

    assertThat(fileToAdd.length()).isGreaterThan(insightConfig.getSupportConfig().getReadLimitBytes());
    final List<SupportFile> filesToZip = new ArrayList<>();
    filesToZip.add(new SupportFile(SupportFileType.CONFIG, fileToAdd, false));

    supportService.populateZip(prefix, supportZip, filesToZip, noLimit);
    return supportZip;
  }

  @Test
  public void testPopulateZip_Limit() throws Exception {
    final File fileToAdd = getConfigYml();

    final File supportZip = createPopulatedZip(false, fileToAdd);

    try (final ZipFile zipFile = new ZipFile(supportZip)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      final ZipEntry firstEntry = entries.nextElement();
      assertThat(firstEntry.getName()).isEqualTo(
          getZipFileBasename(supportZip) + "/" + SupportFileType.CONFIG.getDirName() + "/" + CONFIG_YML_FILENAME);
      assertThat(firstEntry.getSize()).isEqualTo(
          // expected size includes the limit size, plus the appended "Truncated" message, plus a newline
          insightConfig.getSupportConfig().getReadLimitBytes() + (SupportService.TRUNCATED_TOKEN + "\n").length());

      assertThat(entries.nextElement().getName()).isEqualTo(getZipFileBasename(supportZip) + "/truncated");

      assertThat(entries.hasMoreElements()).isFalse();
    }
  }

  @Test
  public void testPopulateZip_NoLimit() throws Exception {
    final File fileToAdd = getConfigYml();

    final File supportZip = createPopulatedZip(true, fileToAdd);

    try (final ZipFile zipFile = new ZipFile(supportZip)) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      final ZipEntry firstEntry = entries.nextElement();
      assertThat(firstEntry.getName()).isEqualTo(
          getZipFileBasename(supportZip) + "/" + SupportFileType.CONFIG.getDirName() + "/" + CONFIG_YML_FILENAME);
      assertThat(firstEntry.getSize()).isEqualTo(fileToAdd.length());

      assertThat(entries.hasMoreElements()).isFalse();
    }
  }
}

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
import org.junit.Test;

import static com.sonatype.insight.brain.support.LimitedFileInputStreamTest.CONFIG_YML;
import static com.sonatype.insight.brain.support.LimitedFileInputStreamTest.CONFIG_YML_FILENAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;

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

  @Test
  public void testCreateSupportZip() throws Exception {
    assertThat(supportService.createSupportZip(false, null), notNullValue());
  }

  @Test
  public void testCreateSupportZipFilenameRules() throws Exception {
    final String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    final String nowPrefix = now.substring(0, now.indexOf("-"));

    final File firstZip = supportService.createSupportZip(false, null);
    final String firstFilename = firstZip.getName();
    assertThat(firstFilename, startsWith("support-" + nowPrefix));
    final int zipIndex = firstFilename.indexOf(".zip");
    final int counterValue = Integer.parseInt(firstFilename.substring(zipIndex - 1, zipIndex));

    final File secondZip = supportService.createSupportZip(false, null);
    assertThat(secondZip.getName(), startsWith("support-" + nowPrefix));
    assertThat(secondZip.getName(), endsWith(("-" + (counterValue + 1) + ".zip")));
  }

  @Test
  public void testCreateSupportZip_UsesSubDir() throws Exception {
    supportService.createSupportZip(false, null);
    assertThat(supportService.getWorkDir().exists(), is(true));
  }

  @Test
  public void testCreateSupportZip_DeletesFilteredFile() throws Exception {
    final File configYml = new File(SupportServiceTest.class.getResource(CONFIG_YML).getFile());
    final File origArg = InsightBrainService.getConfigFile();
    try {
      InsightBrainService.setConfigFile(configYml);
      supportService.createSupportZip(false, null);
      final File filteredConfigYml = new File(supportService.getWorkDir(), "filtered-" + configYml.getName());
      assertThat(filteredConfigYml.exists(), is(false));
    }
    finally {
      InsightBrainService.setConfigFile(origArg);
    }
  }

  @Test
  public void testCreateSupportZip_TruncatedFileStartsWithToken() throws Exception {
    final File configYml = new File(SupportServiceTest.class.getResource(CONFIG_YML).getFile());
    final File origArg = InsightBrainService.getConfigFile();
    try {
      InsightBrainService.setConfigFile(configYml);

      insightConfig.getSupportConfig().setReadLimitBytes(500);

      final File supportZip = supportService.createSupportZip(false, null);
      // read file from zip and assert token suffix
      try (final ZipFile zipFile = new ZipFile(supportZip)) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        final ZipEntry zipEntry = entries.nextElement();
        assertThat(zipEntry.getName(),
            is(getZipFileBasename(supportZip) + "/" + SupportFileType.CONFIG.getDirName() + "/filtered-" +
                CONFIG_YML_FILENAME));
        try (final ByteArrayOutputStream zipEntryContent = new ByteArrayOutputStream()) {
          try (final InputStream zipEntryStream = zipFile.getInputStream(zipEntry)) {
            IOUtil.copy(zipEntryStream, zipEntryContent);
          }
          assertThat(zipEntryContent.toString("UTF-8"), startsWith(SupportService.TRUNCATED_TOKEN));
        }
      }
    }
    finally {
      InsightBrainService.setConfigFile(origArg);
    }
  }

  @Test
  public void testCreateSupportZip_TruncatedZipIncludesTruncatedEntry() throws Exception {
    final File configYml = new File(SupportServiceTest.class.getResource(CONFIG_YML).getFile());
    final File origArg = InsightBrainService.getConfigFile();
    try {
      InsightBrainService.setConfigFile(configYml);

      insightConfig.getSupportConfig().setReadLimitBytes(5);

      final File supportZip = supportService.createSupportZip(false, null);
      // read zip and assert truncated entry
      try (final ZipFile zipFile = new ZipFile(supportZip)) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        assertThat(entries.nextElement().getName(),
            is(getZipFileBasename(supportZip) + "/" + SupportFileType.CONFIG.getDirName() + "/filtered-" +
                CONFIG_YML_FILENAME));
        verifyRequiredEntries(supportZip, entries);
        assertThat(entries.nextElement().toString(), is(getZipFileBasename(supportZip) + "/" + "truncated"));
      }
    }
    finally {
      InsightBrainService.setConfigFile(origArg);
    }
  }

  private static void verifyRequiredEntries(final File supportZipFile, final Enumeration<? extends ZipEntry> entries) {
    final String zipFileBasename = getZipFileBasename(supportZipFile);

    assertThat(entries.nextElement().getName(),
        is(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/sysinfo.json"));
    assertThat(entries.nextElement().getName(),
        is(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/product-version.json"));
    assertThat(entries.nextElement().getName(),
        is(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/product-license.json"));
    assertThat(entries.nextElement().getName(),
        is(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/threads.txt"));
    assertThat(entries.nextElement().getName(),
        is(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/jmx.json"));
    assertThat(entries.nextElement().getName(),
        is(zipFileBasename + "/" + SupportFileType.CONFIG.getDirName() + "/ldap.json"));
    assertThat(entries.nextElement().getName(),
        is(zipFileBasename + "/" + SupportFileType.INFO.getDirName() + "/dbFileInfo.txt"));
  }

  private static String getZipFileBasename(final File supportZipFile) {
    return supportZipFile.getName().substring(0, supportZipFile.getName().length() - 4);
  }

  @Test
  public void testCreateSupportZip_NoConfigFile() throws Exception {
    final File configYml = new File("config-I-dont-exist.yml");
    final File origArg = InsightBrainService.getConfigFile();
    try {
      InsightBrainService.setConfigFile(configYml);
      final File supportZip = supportService.createSupportZip(false, null);
      // read file from zip and assert no config file entry
      try (final ZipFile zipFile = new ZipFile(supportZip)) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        verifyRequiredEntries(supportZip, entries);
        assertThat(entries.hasMoreElements(), is(false));
      }
    }
    finally {
      InsightBrainService.setConfigFile(origArg);
    }
  }

  @Test
  public void testCreateSupportZip_HasRequiredEntries() throws Exception {
    final File origArg = InsightBrainService.getConfigFile();
    try {
      InsightBrainService.setConfigFile(new File(SupportServiceTest.class.getResource(CONFIG_YML).getFile()));
      final File supportZip = supportService.createSupportZip(false, null);
      try (final ZipFile zipFile = new ZipFile(supportZip)) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        if (InsightBrainService.getConfigFile() != null) {
          assertThat(entries.nextElement().getName(),
              is(getZipFileBasename(supportZip) + "/" + SupportFileType.CONFIG.getDirName() +
                  "/filtered-" + CONFIG_YML_FILENAME));
        }
        verifyRequiredEntries(supportZip, entries);
        assertThat(entries.hasMoreElements(), is(false));
      }
    }
    finally {
      InsightBrainService.setConfigFile(origArg);
    }
  }

  @Test
  public void testCreateSupportZip_HasEntryProductVersionSorted() throws Exception {
    final File origArg = InsightBrainService.getConfigFile();
    try {
      InsightBrainService.setConfigFile(new File(SupportServiceTest.class.getResource(CONFIG_YML).getFile()));
      final File supportZip = supportService.createSupportZip(false, null);
      try (final ZipFile zipFile = new ZipFile(supportZip)) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        if (InsightBrainService.getConfigFile() != null) {
          assertThat(entries.nextElement().getName(),
              is(getZipFileBasename(supportZip) + "/" + SupportFileType.CONFIG.getDirName() +
                  "/filtered-" + CONFIG_YML_FILENAME));
        }
        assertThat(entries.nextElement().toString(),
            is(getZipFileBasename(supportZip) + "/" + SupportFileType.INFO.getDirName() + "/sysinfo.json"));

        final ZipEntry zipEntry = entries.nextElement();
        assertThat(zipEntry.toString(),
            is(getZipFileBasename(supportZip) + "/" + SupportFileType.INFO.getDirName() + "/product-version.json"));
        try (final ByteArrayOutputStream zipEntryContent = new ByteArrayOutputStream()) {
          try (final InputStream zipEntryStream = zipFile.getInputStream(zipEntry)) {
            IOUtil.copy(zipEntryStream, zipEntryContent);
            final JsonNode result = JsonUtils.parse(zipEntryContent.toString());
            assertThat(result.size(), is(1));
            final JsonNode parentNode = result.get("product-version");
            final Iterator<String> children = parentNode.fieldNames();
            assertThat(children.next(), is("build"));
            assertThat(children.next(), is("name"));
            assertThat(children.next(), is("tag"));
            assertThat(children.next(), is("timestamp"));
            assertThat(children.next(), is("version"));
            assertThat(parentNode.size(), is(5));
          }
        }
      }
    }
    finally {
      InsightBrainService.setConfigFile(origArg);
    }
  }

  @Test
  public void testAddAllDbData() throws IOException {
    final List<SupportFile> filesToZip = new ArrayList<>();
    final File workDir = tempDir.newFolder("dbDataTest");
    supportService.addAllDbData(filesToZip, workDir);

    final String[] basenames = new String[]{
        "schemaInfo",
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
    assertThat(workDir.listFiles(), arrayContainingInAnyOrder(expectedFiles));
  }

  private File[] createExpectedFiles(final File workDir, final String[] basenames) {
    final File[] expectedFiles = new File[basenames.length];
    for (int i = 0; i < basenames.length; i++) {
      expectedFiles[i] = new File(workDir, basenames[i] + ".json");
    }
    return expectedFiles;
  }
}

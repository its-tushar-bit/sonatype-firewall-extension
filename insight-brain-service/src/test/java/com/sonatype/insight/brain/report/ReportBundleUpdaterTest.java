/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.sonatype.insight.brain.report.ReportBundleUpdater.FilenameMapping;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportBundleUpdaterTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private File originalFile;

  private File updatedFile;

  private Map<String, String> read(File zipFile) throws Exception {
    Map<String, String> contents = new LinkedHashMap<>();
    try (ZipFile zip = new ZipFile(zipFile)) {
      for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements();) {
        ZipEntry entry = en.nextElement();
        try (InputStream in = zip.getInputStream(entry)) {
          contents.put(entry.getName(), IOUtil.toString(in, "UTF-8"));
        }
      }
    }
    return contents;
  }

  @Before
  public void init() throws Exception {
    originalFile = tmpDir.newFile();
    updatedFile = new File(tmpDir.getRoot(), "not-yet-existent/report.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(originalFile))) {
      ZipEntry entry = new ZipEntry("one.txt");
      zos.putNextEntry(entry);
      zos.write("test".getBytes(StandardCharsets.UTF_8));
      entry = new ZipEntry("two.html");
      zos.putNextEntry(entry);
      zos.write("test".getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  public void testAdd_File() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("added.pdf", tmpDir.newFile());
    }
    assertThat(read(updatedFile).keySet()).containsExactlyInAnyOrder("one.txt", "two.html", "added.pdf");
    assertThat(read(updatedFile).get("added.pdf")).isEqualTo("");
  }

  @Test
  public void testAdd_Bytes() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("added.pdf", "added".getBytes(StandardCharsets.UTF_8));
    }
    assertThat(read(updatedFile).keySet()).containsExactlyInAnyOrder("one.txt", "two.html", "added.pdf");
    assertThat(read(updatedFile).get("added.pdf")).isEqualTo("added");
  }

  @Test
  public void testAdd_Dto() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("added.pdf", true);
    }
    assertThat(read(updatedFile).keySet()).containsExactlyInAnyOrder("one.txt", "two.html", "added.pdf");
    assertThat(read(updatedFile).get("added.pdf")).isEqualTo("true");
  }

  @Test
  public void testContains() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      assertThat(updater.contains("added.pdf")).isFalse();
      updater.add("added.pdf", tmpDir.newFile());
      assertThat(updater.contains("added.pdf")).isTrue();
    }
  }

  @Test
  public void testOverwrite() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("one.txt", tmpDir.newFile());
    }
    assertThat(read(updatedFile).get("one.txt")).isEqualTo("");
  }

  @Test
  public void testOverwrite_WithFilenameMapping() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile, new FilenameMapping(
        ".*\\.txt", "data/$0"))) {
      updater.add("data/one.txt", tmpDir.newFile());
    }
    assertThat(read(updatedFile).get("data/one.txt")).isEqualTo("");
    assertThat(read(updatedFile).get("one.txt")).isNull();
  }

  @Test
  public void testRemove() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.remove("one.txt");
    }
    assertThat(read(updatedFile).keySet()).containsExactlyInAnyOrder("two.html");
  }

  @Test
  public void testRemove_WithFilenameMapping() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile, new FilenameMapping(
        ".*\\.txt", "data/$0"))) {
      updater.remove("data/one.txt");
    }
    assertThat(read(updatedFile).keySet()).containsExactlyInAnyOrder("two.html");
  }
}

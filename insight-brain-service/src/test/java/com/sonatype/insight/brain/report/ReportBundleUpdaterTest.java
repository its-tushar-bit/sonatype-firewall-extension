/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
      zos.write("test".getBytes("UTF-8"));
      entry = new ZipEntry("two.html");
      zos.putNextEntry(entry);
      zos.write("test".getBytes("UTF-8"));
    }
  }

  @Test
  public void testAdd_File() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("added.pdf", tmpDir.newFile());
    }
    assertThat(read(updatedFile).keySet(), containsInAnyOrder("one.txt", "two.html", "added.pdf"));
    assertThat(read(updatedFile).get("added.pdf"), is(""));
  }

  @Test
  public void testAdd_Bytes() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("added.pdf", "added".getBytes("UTF-8"));
    }
    assertThat(read(updatedFile).keySet(), containsInAnyOrder("one.txt", "two.html", "added.pdf"));
    assertThat(read(updatedFile).get("added.pdf"), is("added"));
  }

  @Test
  public void testAdd_Dto() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("added.pdf", true);
    }
    assertThat(read(updatedFile).keySet(), containsInAnyOrder("one.txt", "two.html", "added.pdf"));
    assertThat(read(updatedFile).get("added.pdf"), is("true"));
  }

  @Test
  public void testContains() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      assertThat(updater.contains("added.pdf"), is(false));
      updater.add("added.pdf", tmpDir.newFile());
      assertThat(updater.contains("added.pdf"), is(true));
    }
  }

  @Test
  public void testOverwrite() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.add("one.txt", tmpDir.newFile());
    }
    assertThat(read(updatedFile).get("one.txt"), is(""));
  }

  @Test
  public void testRemove() throws Exception {
    try (ReportBundleUpdater updater = new ReportBundleUpdater(originalFile, updatedFile)) {
      updater.remove("one.txt");
    }
    assertThat(read(updatedFile).keySet(), containsInAnyOrder("two.html"));
  }
}

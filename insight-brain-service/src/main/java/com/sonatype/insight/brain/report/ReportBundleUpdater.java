/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.sonatype.insight.json.store.JsonUtils;

import org.codehaus.plexus.util.IOUtil;

/**
 * Assists in augmenting the report bundle downloaded from the HDS.
 */
class ReportBundleUpdater
    implements Closeable
{

  private final File originalFile;

  private final ZipOutputStream zipStream;

  private final Collection<String> addedEntries;

  private final Collection<String> removedEntries;

  /**
   * Creates an updater for the specified {@code report.zip}, using the given location for the updated bundle.
   */
  public ReportBundleUpdater(File originalFile, File updatedFile) throws IOException {
    this.originalFile = originalFile;
    updatedFile.getParentFile().mkdirs();
    zipStream = new ZipOutputStream(new FileOutputStream(updatedFile));
    addedEntries = new HashSet<>();
    removedEntries = new HashSet<String>();
  }

  /**
   * Removes the specified entry from the updated bundle.
   */
  public void remove(String entryName) {
    removedEntries.add(entryName);
  }

  /**
   * Adds the specified file into the bundle using the given name.
   */
  public void add(String entryName, File srcFile) throws IOException {
    ZipEntry zipEntry = new ZipEntry(entryName);
    zipStream.putNextEntry(zipEntry);
    try (InputStream in = new FileInputStream(srcFile)) {
      IOUtil.copy(in, zipStream);
    }
    addedEntries.add(entryName);
  }

  /**
   * Adds the specified binary blob into the bundle using the given name.
   */
  public void add(String entryName, byte[] bytes) throws IOException {
    ZipEntry zipEntry = new ZipEntry(entryName);
    zipStream.putNextEntry(zipEntry);
    zipStream.write(bytes);
    addedEntries.add(entryName);
  }

  /**
   * Adds the specified DTO as JSON into the bundle using the given name.
   */
  public void add(String entryName, Object dto) throws IOException {
    ZipEntry zipEntry = new ZipEntry(entryName);
    zipStream.putNextEntry(zipEntry);
    zipStream.write(JsonUtils.generate(dto));
    addedEntries.add(entryName);
  }

  @Override
  public void close() throws IOException {
    try {
      try (ZipFile zipFile = new ZipFile(originalFile)) {
        for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
          ZipEntry entry = entries.nextElement();
          String entryName = entry.getName();
          if (!addedEntries.contains(entryName) && !removedEntries.contains(entryName)) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipStream.putNextEntry(zipEntry);
            try (InputStream in = zipFile.getInputStream(entry)) {
              IOUtil.copy(in, zipStream);
            }
          }
        }
      }
    }
    finally {
      zipStream.close();
    }
  }
}

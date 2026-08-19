/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.io.IOUtils;

/**
 * Assists in augmenting the report bundle downloaded from the HDS.
 */
class ReportBundleUpdater
    implements Closeable
{
  /**
   * Renames entries from the original report before inclusion in the updated bundle.
   */
  public static class FilenameMapping
  {
    private final Pattern pattern;

    private final String replacement;

    public FilenameMapping(String regexp, String replacement) {
      this.pattern = Pattern.compile(regexp);
      this.replacement = replacement;
    }

    public String apply(String entryName) {
      return pattern.matcher(entryName).replaceAll(replacement);
    }
  }

  private final Stream<ReportEntity> reportEntities;

  private final ZipOutputStream zipStream;

  private final Collection<String> addedEntries;

  private final Collection<String> removedEntries;

  private final Collection<FilenameMapping> filenameMappings;

  /**
   * Creates an updater for the specified {@code report.zip}, using the given location for the updated bundle. The
   * supplied filename mapping chain allows to move/rename files from the original report.
   */
  public ReportBundleUpdater(
      Stream<ReportEntity> originalReportEntities,
      File updatedFile,
      FilenameMapping... filenameMappings) throws IOException
  {
    this.reportEntities = originalReportEntities;
    updatedFile.getParentFile().mkdirs();
    zipStream = new ZipOutputStream(new FileOutputStream(updatedFile));
    addedEntries = new HashSet<>();
    removedEntries = new HashSet<>();
    this.filenameMappings = Arrays.asList(filenameMappings);
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
      IOUtils.copy(in, zipStream);
    }
    addedEntries.add(entryName);
  }

  /**
   * To be used only by {@link ReportResource}.
   */
  void add(String entryName, InputStream inputStream) throws IOException {
    ZipEntry zipEntry = new ZipEntry(entryName);
    zipStream.putNextEntry(zipEntry);
    IOUtils.copy(inputStream, zipStream);
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

  /**
   * Checks whether the given name has already been added to the update set.
   */
  public boolean contains(String entryName) {
    return addedEntries.contains(entryName);
  }

  @Override
  public void close() throws IOException {
    try {
      addRemainingOriginalReportEntities();
    }
    finally {
      zipStream.close();
    }
  }

  /**
   * Add entities from the original report that were not removed or added by explicit calls to this bundle updater
   */
  private void addRemainingOriginalReportEntities() throws IOException {
    try {
      reportEntities.forEach(entity -> {
        String entryName = applyFilenameMappings(entity.getName());
        if (!addedEntries.contains(entryName) && !removedEntries.contains(entryName)) {
          ZipEntry zipEntry = new ZipEntry(entryName);
          try {
            zipStream.putNextEntry(zipEntry);
            try (var inputStream = entity.getInputStream()) {
              IOUtils.copy(inputStream, zipStream);
            }
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }
      });
    }
    catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private String applyFilenameMappings(String entryName) {
    for (FilenameMapping filenameMapping : filenameMappings) {
      entryName = filenameMapping.apply(entryName);
    }
    return entryName;
  }
}

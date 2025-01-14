/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.report.pdf.PdfGenerator;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.annotations.VisibleForTesting;
import datadog.trace.api.Trace;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

public class FileApplicationReport
    extends AbstractFileReportEntity
    implements ApplicationReport
{
  private static final Logger log = LoggerFactory.getLogger(FileApplicationReport.class);

  public static final String CACHE_DIRECTORY_NAME = "report.cache";

  /*
  Files were previously added to the report.zip. Instead of adding to the zip, keep additional files in a separate
  directory instead of the report.cache directory so that they're not removed when running a re-evaluation. These should
  be considered just as durable as the report.zip itself
*/
  public static final String ADDITIONAL_FILES_DIRECTORY_NAME = "additional.files";

  public FileApplicationReport(final File file) {
    super(file);
  }

  @Override
  public String getLocation() {
    return file.getAbsolutePath();
  }

  @Trace
  @Override
  public ReportEntry getEntry(final String name) throws IOException {
    if (name.contains("../") || name.contains("..\\")) {
      // legit callers use normalized paths, no directory traversal into restricted areas
      return null;
    }
    final File cacheFile = getCacheFile(name);
    if (cacheFile.canRead()) {
      return new ReportEntry(name, cacheFile.lastModified(), fetch(cacheFile));
    }
    final File additionalFile = getAdditionalFile(name);
    if (additionalFile.canRead()) {
      return new ReportEntry(name, additionalFile.lastModified(), fetch(additionalFile));
    }
    return extractEntry(name);
  }

  @Trace
  @Override
  public void putEntry(final String name, final byte[] buf) throws IOException {
    cache(getCacheFile(name), buf);
  }

  @Trace
  @Override
  public void saveReportEntry(String entryFileName, ContainerNode<?> jsonData) throws IOException {
    long start = System.currentTimeMillis();

    putEntry(entryFileName, JsonUtils.generate(jsonData));

    log.debug("saveReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);
  }

  @Trace
  @Override
  public ContainerNode<?> loadReportEntry(String entryFileName) throws IOException {
    long start = System.currentTimeMillis();

    ReportEntry reportEntry = extractEntry(entryFileName);
    ContainerNode<?> result = JsonUtils.parse(reportEntry.buf);

    log.debug("loadReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);

    return result;
  }

  @Trace
  private byte[] fetch(final File cacheFile) throws IOException {
    return Files.readAllBytes(cacheFile.toPath());
  }

  @Trace
  private ReportEntry extractEntry(final String name) throws IOException {
    // When the archive is closed, all InputStreams retrieved from this archive are also closed.
    try (final ZipFile archive = new ZipFile(file)) {
      final ZipEntry entry = archive.getEntry(name);
      if (entry != null) {
        final byte[] buf = IOUtils.toByteArray(archive.getInputStream(entry));
        return new ReportEntry(entry.getName(), entry.getTime(), buf);
      }
    }

    // Starting with release 1.168, we serve shared resources for legacy report from the jar
    // HDS does not include these files in the report.zip when IQ client is v1.168 or higher
    String resource = "/com/sonatype/insight/brain/legacy.report/" + name;
    try (InputStream stream = FileApplicationReport.class.getResourceAsStream(resource)) {
      if (stream != null) {
        return new ReportEntry(name, new Date().getTime(), IOUtils.toByteArray(stream));
      }
    }

    return null;
  }

  @VisibleForTesting
  public File getCacheFile(final String name) {
    File f = new File(getCacheDir(file), name);
    log.trace("Cache file: {}", f.getAbsolutePath());
    return f;
  }

  @Trace
  public File getAdditionalFile(final String name) {
    File f = new File(getOrCreateAdditionalFilesDir(file), name);
    log.trace("Report entry file: {}", f.getAbsolutePath());
    return f;
  }

  static File getCacheDir(final File reportFile) {
    File file = new File(reportFile.getParentFile(), CACHE_DIRECTORY_NAME);
    log.trace("Cache dir: {}", file.getAbsolutePath());
    return file;
  }

  static File getOrCreateAdditionalFilesDir(final File reportFile) {
    File file = new File(reportFile.getParentFile(), ADDITIONAL_FILES_DIRECTORY_NAME);
    file.mkdirs();
    log.trace("Report Files dir: {}", file.getAbsolutePath());
    return file;
  }

  @Trace
  private void cache(final File f, final byte[] buf) throws IOException {
    Files.createDirectories(f.getAbsoluteFile().getParentFile().toPath());
    Files.write(f.toPath(), buf);
  }

  @Trace
  @Override
  public void deletePdfReport() {
    File pdfReportFile = new File(file.getParentFile(), PdfGenerator.REPORT_FILE_NAME);
    try {
      if (Files.deleteIfExists(pdfReportFile.toPath())) {
        log.debug("Deleted obsolete PDF report file: {}.", pdfReportFile.getAbsolutePath());
      }
    }
    catch (Exception e) {
      log.error("Cannot delete obsolete PDF report file: {}. Cause: {}", pdfReportFile.getAbsolutePath(),
          e.getMessage(), e);
    }
  }

  @Trace
  @Override
  public void appendToReport(final ThirdPartyApplicationReportDTO dto) throws IOException {
    appendFileToReport(THIRD_PARTY_BOM_JSON_FILENAME, dto.billOfMaterials);
    appendFileToReport(THIRD_PARTY_SECURITY_JSON_FILENAME, dto.securityRows);
    appendFileToReport(THIRD_PARTY_LICENSE_JSON_FILENAME, dto.licenseRows);
  }

  private void appendFileToReport(final String filename, final List<?> data)
      throws IOException
  {
    Path newFile = new File(getOrCreateAdditionalFilesDir(file), filename).toPath();
    try (var writer = Files.newBufferedWriter(newFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      writer.write(new String(JsonUtils.generate(JsonUtils.aaData(data)), StandardCharsets.UTF_8));
    }
  }

  @Trace
  @Override
  public ReportType getType() throws IOException {
    try (final ZipFile archive = new ZipFile(file)) {
      if (archive.getEntry(SECURITY_JSON_FILENAME) == null && archive.getEntry(LICENSES_JSON_FILENAME) == null) {
        return ReportType.ERROR;
      }
      return ReportType.FULL;
    }
  }

  @Trace
  @Override
  public void deleteCacheDir() throws FileDeletionException {
    new FileCleaner().delete(getCacheDir(file));
  }

  @Trace
  @Override
  public Properties getTemplateProperties() throws IOException {
    try (ZipFile archive = new ZipFile(file)) {
      Properties props = new Properties();
      ZipEntry entry = archive.getEntry("template.properties");
      if (entry != null) {
        props.load(archive.getInputStream(entry));
      }
      return props;
    }
  }
}

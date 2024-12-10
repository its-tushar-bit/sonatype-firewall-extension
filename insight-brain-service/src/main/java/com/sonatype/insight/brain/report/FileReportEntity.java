/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.report.pdf.PdfGenerator;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

public class FileReportEntity
    implements ReportPdf, ApplicationReport
{
  private static final Logger log = LoggerFactory.getLogger(FileReportEntity.class);

  public static final String CACHE_DIRECTORY_NAME = "report.cache";

  private final File file;

  public FileReportEntity(final File file) {
    this.file = file;
  }

  public File getFile() {
    return file;
  }

  @Override
  public boolean exists() {
    return file.exists();
  }

  @Override
  public void deleteIfExists() throws IOException {
    Files.deleteIfExists(file.toPath());
  }

  @Override
  public boolean canCreate() {
    return !file.isFile() || file.length() == 0;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return new FileOutputStream(file);
  }

  @Override
  public String getLocation() {
    return file.getAbsolutePath();
  }

  @Override
  public long length() {
    return file.length();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public ReportEntry getEntry(final String name) throws IOException  {
    if (name.contains("../") || name.contains("..\\")) {
      // legit callers use normalized paths, no directory traversal into restricted areas
      return null;
    }
    final File cacheFile = getCacheFile(name);
    if (cacheFile.canRead()) {
      return new ReportEntry(name, cacheFile.lastModified(), fetch(cacheFile));
    }
    return extractEntry(name);
  }

  @Override
  public void putEntry(final String name, final byte[] buf) throws IOException {
    cache(getCacheFile(name), buf);
  }

  @Override
  public void putEntry(final String name, final String text) throws IOException {
    putEntry(name, text.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void saveReportEntry(String entryFileName, ContainerNode<?> jsonData)
      throws IOException
  {
    long start = System.currentTimeMillis();

    cache(getCacheFile(entryFileName), JsonUtils.generate(jsonData));

    log.debug("saveReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);
  }

  @Override
  public ContainerNode<?> loadReportEntry(String entryFileName) throws IOException {
    long start = System.currentTimeMillis();

    ReportEntry reportEntry = extractEntry(entryFileName);
    ContainerNode<?> result = JsonUtils.parse(reportEntry.buf);

    log.debug("loadReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);

    return result;
  }

  private byte[] fetch(final File cacheFile) throws IOException {
    return Files.readAllBytes(cacheFile.toPath());
  }

  @Override
  public ReportEntry extractEntry(final String name) throws IOException {
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
    try (InputStream stream = FileReportEntity.class.getResourceAsStream(resource)) {
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

  static File getCacheDir(final File reportFile) {
    File file = new File(reportFile.getParentFile(), CACHE_DIRECTORY_NAME);
    log.trace("Cache dir: {}", file.getAbsolutePath());
    return file;
  }

  private void cache(final File f, final byte[] buf) throws IOException {
    Files.createDirectories(f.getAbsoluteFile().getParentFile().toPath());
    Files.write(f.toPath(), buf);
  }

  @Override
  public void embedApplicationPublicId(Application application) throws IOException {
    String filename = "index.html";
    ReportEntry reportEntry = extractEntry(filename);
    String originalIndexHtmlContent = new String(reportEntry.buf, StandardCharsets.UTF_8);
    String augmentedIndexHtmlContent = originalIndexHtmlContent.replace("applicationId = ''", "applicationId = '"
        + application.getPublicId() + "'");
    if (!augmentedIndexHtmlContent.equals(originalIndexHtmlContent)) {
      cache(getCacheFile(filename), augmentedIndexHtmlContent.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Override
  public void cacheThirdPartyData() {
    THIRD_PARTY_CACHED_FILES.forEach(filename -> {
      try {
        final ReportEntry entry = getEntry(filename);
        if (entry != null) {
          cache(getCacheFile(filename), entry.buf);
        }
      }
      catch (IOException e) {
        log.error("Error reading third party data from report file: {}", getLocation(), e);
      }
    });
  }

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

  @Override
  public void appendToReport(final ThirdPartyApplicationReportDTO dto)
      throws IOException
  {
    Map<String, Object> env = new HashMap<>();
    env.put("create", "false");
    env.put("useTempFile", Boolean.TRUE); //to avoid large byte streams created in memory
    Path archivePath = file.toPath();
    URI archiveUri = URI.create("jar:" + archivePath.toUri());
    try (FileSystem fs = FileSystems.newFileSystem(archiveUri, env)) {
      appendFileToReportZip(fs, THIRD_PARTY_BOM_JSON_FILENAME, dto.billOfMaterials);
      appendFileToReportZip(fs, THIRD_PARTY_SECURITY_JSON_FILENAME, dto.securityRows);
      appendFileToReportZip(fs, THIRD_PARTY_LICENSE_JSON_FILENAME, dto.licenseRows);
    }
  }

  private void appendFileToReportZip(final FileSystem fs, final String filename, final List<?> data)
      throws IOException
  {
    Path newFile = fs.getPath(filename);
    try (Writer writer = Files.newBufferedWriter(newFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
      writer.write(new String(JsonUtils.generate(JsonUtils.aaData(data)), StandardCharsets.UTF_8));
    }
  }

  @Override
  public ReportType getType() throws IOException {
    try (final ZipFile archive = new ZipFile(file)) {
      if (archive.getEntry(SECURITY_JSON_FILENAME) == null && archive.getEntry(LICENSES_JSON_FILENAME) == null) {
        return ReportType.ERROR;
      }
      return ReportType.FULL;
    }
  }

  @Override
  public void deleteCacheDir() throws FileDeletionException {
    new FileCleaner().delete(getCacheDir(file));
  }

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

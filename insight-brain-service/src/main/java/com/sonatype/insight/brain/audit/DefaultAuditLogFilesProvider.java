/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
@Singleton
public class DefaultAuditLogFilesProvider
    implements AuditLogFilesProvider
{
  private static final String DEFAULT_AUDIT_LOG_NAME = "audit.log";

  private final InsightConfig config;

  @Inject
  public DefaultAuditLogFilesProvider(final InsightConfig config) {
    this.config = config;
  }

  @Override
  public List<File> getAuditLogFiles(final LocalDate startUtcDate, final LocalDate endUtcDate) {
    AuditLogDirectory auditLogDirectory = getAuditLogDirectory();
    if (auditLogDirectory == null) {
      throw new BadRequestException("Cannot get the audit log path.");
    }

    try (Stream<Path> stream = Files.list(auditLogDirectory.path())) {
      return stream
          .filter(path -> filterForAuditLogs(path, startUtcDate, endUtcDate, auditLogDirectory.currentAuditLogName()))
          .map(Path::toFile)
          .sorted()
          .collect(Collectors.toList());
    }
    catch (IOException e) {
      throw new UncheckedIOException("Error listing audit log files: " + e.getMessage(), e);
    }
  }

  private boolean filterForAuditLogs(
      final Path path,
      final LocalDate startUtcDate,
      final LocalDate endUtcDate,
      final String currentAuditLogName)
  {
    if (Files.isDirectory(path)) {
      return false;
    }

    String fileName = path.getFileName().toString();
    if (fileName.equals(currentAuditLogName) && endUtcDate.isEqual(LocalDate.now())) {
      return true;
    }

    LocalDate fileDate = parseArchiveDate(fileName, currentAuditLogName);
    if (fileDate == null && !DEFAULT_AUDIT_LOG_NAME.equals(currentAuditLogName)) {
      fileDate = parseArchiveDate(fileName, DEFAULT_AUDIT_LOG_NAME);
    }
    if (fileDate == null) {
      return false;
    }

    return !fileDate.isBefore(startUtcDate) && !fileDate.isAfter(endUtcDate);
  }

  private LocalDate parseArchiveDate(final String fileName, final String currentAuditLogName) {
    String archivePrefix = archivePrefix(currentAuditLogName);
    String archiveSuffix = archiveSuffix(currentAuditLogName);
    if (!fileName.startsWith(archivePrefix) || !fileName.endsWith(archiveSuffix)) {
      return null;
    }

    String datePortion = fileName.substring(archivePrefix.length(), fileName.length() - archiveSuffix.length());
    try {
      return LocalDate.parse(datePortion);
    }
    catch (DateTimeParseException e) {
      return null;
    }
  }

  private String archivePrefix(final String currentAuditLogName) {
    if (currentAuditLogName.endsWith(".log")) {
      return currentAuditLogName.substring(0, currentAuditLogName.length() - ".log".length()) + "-";
    }
    int extensionIndex = currentAuditLogName.lastIndexOf('.');
    if (extensionIndex > 0) {
      return currentAuditLogName.substring(0, extensionIndex) + "-";
    }
    return currentAuditLogName + "-";
  }

  private String archiveSuffix(final String currentAuditLogName) {
    if (currentAuditLogName.endsWith(".log")) {
      return ".log.gz";
    }
    int extensionIndex = currentAuditLogName.lastIndexOf('.');
    if (extensionIndex > 0) {
      return currentAuditLogName.substring(extensionIndex) + ".gz";
    }
    return ".gz";
  }

  private AuditLogDirectory getAuditLogDirectory() {
    File configuredAuditLog = getConfiguredAuditLogFile();
    if (configuredAuditLog != null) {
      File configuredParent = configuredAuditLog.getAbsoluteFile().getParentFile();
      if (configuredParent != null && configuredParent.isDirectory()) {
        return new AuditLogDirectory(configuredParent.toPath(), configuredAuditLog.getName());
      }
    }

    File logDir = new File(config.getSonatypeWork(), "logs");
    if (logDir.exists() && logDir.isDirectory()) {
      return new AuditLogDirectory(logDir.toPath(), DEFAULT_AUDIT_LOG_NAME);
    }
    return null;
  }

  private File getConfiguredAuditLogFile() {
    String auditLogFilename = config.getAuditLogFilename();
    if (auditLogFilename == null || auditLogFilename.isBlank()) {
      return null;
    }
    return new File(auditLogFilename);
  }

  private record AuditLogDirectory(Path path, String currentAuditLogName)
  {
  }
}

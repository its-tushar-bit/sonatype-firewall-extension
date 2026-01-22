/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.dropwizard.logging.common.DefaultLoggingFactory;

@Named
@Singleton
public class DefaultAuditLogFilesProvider
    implements AuditLogFilesProvider
{
  private final InsightConfig config;

  @Inject
  public DefaultAuditLogFilesProvider(final InsightConfig config) {
    this.config = config;
  }

  private static final DateTimeFormatter AUDIT_ARCHIVE_LOG_FORMATTER =
      DateTimeFormatter.ofPattern("'audit-'yyyy-MM-dd'.log.gz'");

  private static final String AUDIT_LOG_NAME = "audit.log";

  @Override
  public List<File> getAuditLogFiles(final LocalDate startUtcDate, final LocalDate endUtcDate) {
    String auditLogParentFolder = getAuditLogParentFolder();
    if (auditLogParentFolder == null) {
      throw new BadRequestException("Cannot get the audit log path.");
    }

    try (Stream<Path> stream = Files.list(Paths.get(auditLogParentFolder))) {
      return stream
          .filter(path -> filterForAuditLogs(path, startUtcDate, endUtcDate))
          .map(Path::toFile)
          .sorted()
          .collect(Collectors.toList());
    }
    catch (IOException e) {
      throw new UncheckedIOException("Error listing audit log files: " + e.getMessage(), e);
    }
  }

  private static boolean filterForAuditLogs(final Path path, final LocalDate startUtcDate, final LocalDate endUtcDate) {
    if (Files.isDirectory(path)) {
      return false;
    }

    String fileName = path.getFileName().toString();
    if (fileName.equals(AUDIT_LOG_NAME) && endUtcDate.isEqual(LocalDate.now())) {
      return true;
    }

    try {
      LocalDate fileDate = LocalDate.parse(fileName, AUDIT_ARCHIVE_LOG_FORMATTER);
      if (fileDate.isBefore(startUtcDate) || fileDate.isAfter(endUtcDate)) {
        return false;
      }
    }
    catch (DateTimeParseException e) {
      return false;
    }

    return true;
  }

  private String getAuditLogParentFolder() {
    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) config.getLoggingFactory();
    Map<String, JsonNode> loggers = loggingFactory.getLoggers();
    JsonNode loggerNode = loggers.getOrDefault(AuditRecorder.BASE_LOGGER_NAME, MissingNode.getInstance());
    String currentLogFilename = StreamSupport.stream(loggerNode.path("appenders").spliterator(), false /* parallel */)
        .map(appender -> appender.path("currentLogFilename")).filter(JsonNode::isTextual)
        .map(JsonNode::asText).findFirst().orElse(null);

    if (currentLogFilename == null) {
      return null;
    }

    Path parent = Paths.get(currentLogFilename).getParent();
    if (parent == null) {
      return ".";
    }

    return parent.toString();
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.io.IOUtils;

@Named
@Singleton
public class ApiAuditLogsService
{
  private final AuditLogFilesProvider auditLogFilesProvider;

  @Inject
  public ApiAuditLogsService(final AuditLogFilesProvider auditLogFilesProvider) {
    this.auditLogFilesProvider = auditLogFilesProvider;
  }

  @Authorize(permission = Permission.ACCESS_AUDIT_LOG)
  public StreamingOutput getAuditLogs(final String startUtcDate, final String endUtcDate) {
    validateRequiredFields(startUtcDate, endUtcDate);

    List<File> auditLogFiles =
        auditLogFilesProvider.getAuditLogFiles(LocalDate.parse(startUtcDate), LocalDate.parse(endUtcDate));
    return buildStreamingOut(auditLogFiles);
  }

  private static void validateRequiredFields(final String startUtcDate, final String endUtcDate) {
    if (startUtcDate == null || endUtcDate == null) {
      throw new BadRequestException("startUtcDate and endUtcDate must be defined");
    }

    LocalDate start;
    try {
      start = LocalDate.parse(startUtcDate);
    }
    catch (DateTimeParseException e) {
      throw new BadRequestException(String.format("startUtcDate '%s' is invalid", startUtcDate));
    }

    LocalDate end;
    try {
      end = LocalDate.parse(endUtcDate);
    }
    catch (DateTimeParseException e) {
      throw new BadRequestException(String.format("endUtcDate '%s' is invalid", endUtcDate));
    }

    if (end.isBefore(start)) {
      throw new BadRequestException("startUtcDate must be before endUtcDate");
    }

    if (end.isAfter(LocalDate.now())) {
      throw new BadRequestException("endUtcDate cannot be in the future");
    }
  }

  private static StreamingOutput buildStreamingOut(final List<File> auditLogFiles) {
    return os -> {
      List<InputStream> inputStreams = new ArrayList<>();
      for (File file : auditLogFiles) {
        if (file.getName().endsWith(".gz")) {
          // decompressing
          inputStreams.add(new GZIPInputStream(Files.newInputStream(file.toPath())));
        }
        else {
          // no decompressing
          inputStreams.add(new BufferedInputStream(Files.newInputStream(file.toPath())));
        }
      }

      try (SequenceInputStream sequenceInputStream = new SequenceInputStream(Collections.enumeration(inputStreams))) {
        IOUtils.copy(sequenceInputStream, os);
      }
    };
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultAuditLogFilesProviderTest
{
  private DefaultAuditLogFilesProvider defaultAuditLogFilesProvider;

  @Before
  public void setup() throws Exception {
    String currentLogFilename = getCurrentLogFilename();
    String appendersWithPathAndFileName =
        "{ \"appenders\": [{\"type\": \"file\", \"currentLogFilename\": \"" + currentLogFilename.replace('\\', '/')
            + "\" }] }";
    defaultAuditLogFilesProvider = new DefaultAuditLogFilesProvider(
        buildAuditConfig(AuditRecorder.BASE_LOGGER_NAME, appendersWithPathAndFileName));
  }

  @Test
  public void testGetAuditLogFiles_NoFilesForTheRange() {
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.of(2024, 2, 4),
            LocalDate.of(2024, 2, 4));

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAuditLogFiles_WhenTheRangeIsToday() {
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.now(), LocalDate.now());

    assertThat(getFileNames(result)).containsExactly("audit.log");
  }

  @Test
  public void testGetAuditLogFiles_ThereAreFilesForTheRange() {
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(LocalDate.of(2024, 2, 4),
            LocalDate.of(2024, 2, 8));

    assertThat(getFileNames(result)).containsExactly("audit-2024-02-07.log.gz",
        "audit-2024-02-08.log.gz");
  }

  @Test
  public void testGetAuditLogFiles_OnlyOneFileForTheRange() {
    LocalDate localDate = LocalDate.of(2024, 2, 8);
    List<File> result =
        defaultAuditLogFilesProvider.getAuditLogFiles(localDate, localDate);

    assertThat(getFileNames(result)).containsExactly("audit-2024-02-08.log.gz");
  }

  @Test
  public void testGetAuditLogFiles_AppenderConsole() throws Exception {
    String appendersWithConsole =
        "{ \"appenders\": [{\"type\": \"console\"}] }";
    DefaultAuditLogFilesProvider defaultAuditLogFilesProviderWithConsole =
        new DefaultAuditLogFilesProvider(buildAuditConfig(AuditRecorder.BASE_LOGGER_NAME, appendersWithConsole));

    assertThatThrownBy(() -> defaultAuditLogFilesProviderWithConsole.getAuditLogFiles(LocalDate.MIN, LocalDate.MAX))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot get the audit log path.");
  }

  @Test
  public void testGetAuditLogFiles_AppenderOnlyFileName() throws Exception {
    String appendersWithOnlyNameFile =
        "{ \"appenders\": [{\"type\": \"file\", \"currentLogFilename\": \"audit.log\" }] }";
    DefaultAuditLogFilesProvider defaultAuditLogFilesProviderWithOnlyNameFile =
        new DefaultAuditLogFilesProvider(buildAuditConfig(AuditRecorder.BASE_LOGGER_NAME, appendersWithOnlyNameFile));

    List<File> result = defaultAuditLogFilesProviderWithOnlyNameFile.getAuditLogFiles(LocalDate.MIN, LocalDate.MAX);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAuditLogFiles_WhenTheBaseLoggerNameIsNotInsightAudit() throws Exception {
    String appendersWithConsole =
        "{ \"appenders\": [{\"type\": \"console\"}] }";
    DefaultAuditLogFilesProvider defaultAuditLogFilesProviderWithoutLoggerNameInsightAudit =
        new DefaultAuditLogFilesProvider(buildAuditConfig("NoValidLoggerName", appendersWithConsole));

    assertThatThrownBy(
        () -> defaultAuditLogFilesProviderWithoutLoggerNameInsightAudit.getAuditLogFiles(LocalDate.MIN, LocalDate.MAX))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Cannot get the audit log path.");
  }

  private String getCurrentLogFilename() {
    URL url = getClass().getResource("/" + getClass().getSimpleName() + "/audit.log");
    return new File(url.getFile()).getAbsolutePath();
  }

  private static List<String> getFileNames(final List<File> files) {
    return files.stream()
        .map(File::getName)
        .collect(Collectors.toList());
  }

  private static InsightConfig buildAuditConfig(final String baseLoggerName, final String logString) throws Exception {
    DefaultLoggingFactory loggingFactory = new DefaultLoggingFactory();
    Map<String, JsonNode> logger = new HashMap<>();
    JsonNode loggerNode = new ObjectMapper().readTree(logString);
    logger.put(baseLoggerName, loggerNode);
    loggingFactory.setLoggers(logger);
    InsightConfig config = new InsightConfig();
    config.setLoggingFactory(loggingFactory);
    return config;
  }
}

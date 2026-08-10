/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ApiAuditLogsServiceTest
    extends AbstractComponentH2Test
{
  private static final String AUDIT_CONTENT =
      "{\"timestamp\":\"2024-02-26T15:11:33.316-03:00\",\"username\":\"*SYSTEM\",\"domain\":\"server\"," +
          "\"type\":\"start\",\"data\":{\"serverInstanceId\":\"9144da15-07af-414c-a96e-bea52fc7abb1\"," +
          "\"serverConfigurationFile\":\"/home/config.yml\",\"serverRelease\":\"174.0-SNAPSHOT\"," +
          "\"serverBuild\":\"build-number\",\"processOwner\":\"obarra\"}}\n";

  private static final String AUDIT_2024_02_07_CONTENT =
      "{\"timestamp\":\"2024-02-07T17:56:48.007-03:00\",\"username\":\"*SYSTEM\",\"domain\":\"server\"," +
          "\"type\":\"start\",\"data\":{\"serverInstanceId\":\"e3a2d628-48fb-4be1-8b7e-861bf64b9224\"," +
          "\"serverConfigurationFile\":\"/home/config.yml\",\"serverRelease\":\"173.0-SNAPSHOT\"," +
          "\"serverBuild\":\"build-number\",\"processOwner\":\"obarra\"}}\n";

  private static final String AUDIT_2024_02_08_CONTENT =
      "{\"timestamp\":\"2024-02-08T17:56:48.007-03:00\",\"username\":\"*SYSTEM\",\"domain\":\"server\"," +
          "\"type\":\"start\",\"data\":{\"serverInstanceId\":\"e3a2d628-48fb-4be1-8b7e-861bf64b9224\"," +
          "\"serverConfigurationFile\":\"/home/config.yml\",\"serverRelease\":\"173.0-SNAPSHOT\"," +
          "\"serverBuild\":\"build-number\",\"processOwner\":\"obarra\"}}\n";

  @Inject
  private ApiAuditLogsService apiAuditLogsService;

  @Inject
  private InsightConfig insightConfig;

  private String logDir;

  @BeforeEach
  public void before() throws Exception {
    // Set up sonatypeWork to point to temp directory
    String sonatypeWork = tempDir.getRoot().getAbsolutePath();
    insightConfig.setSonatypeWork(sonatypeWork);

    // Create logs directory under sonatypeWork
    logDir = sonatypeWork + "/logs";
    Files.createDirectory(Paths.get(logDir));
  }

  @Test
  public void testGetAuditLogs_NoLogs() throws Exception {
    StreamingOutput response = apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08");

    assertThat(getResponseContent(response)).isEmpty();
  }

  @Test
  public void testGetAuditLogs_HistoricalAndCurrentAuditLogs() throws Exception {
    copyTestResource("audit-2024-02-07.log.gz");
    copyTestResource("audit-2024-02-08.log.gz");
    copyTestResource("audit.log");

    StreamingOutput response = apiAuditLogsService.getAuditLogs("2024-02-04", LocalDate.now().toString());

    // Trim the expected and actual values to ignore trailing new lines, whcih are different on Windows and Linux.
    String actualContent = getResponseContent(response).trim();
    String expectedContent = (AUDIT_2024_02_07_CONTENT + AUDIT_2024_02_08_CONTENT + AUDIT_CONTENT).trim();
    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  public void testGetAuditLogs_OnlyCurrentAuditLog() throws Exception {
    copyTestResource("audit-2024-02-07.log.gz");
    copyTestResource("audit-2024-02-08.log.gz");
    copyTestResource("audit.log");

    StreamingOutput response = apiAuditLogsService.getAuditLogs(LocalDate.now().toString(), LocalDate.now().toString());

    // Trim the expected and actual values to ignore trailing new lines, whcih are different on Windows and Linux.
    String actualContent = getResponseContent(response).trim();
    String expectedContent = AUDIT_CONTENT.trim();
    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  public void testGetAuditLogs_OnlyHistoricalAuditLog() throws Exception {
    copyTestResource("audit-2024-02-07.log.gz");
    copyTestResource("audit-2024-02-08.log.gz");
    copyTestResource("audit.log");

    StreamingOutput response = apiAuditLogsService.getAuditLogs("2024-02-08", "2024-02-08");

    String actualContent = getResponseContent(response);
    String expectedContent = AUDIT_2024_02_08_CONTENT;
    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  public void testGetAuditLogs_StripsNulBytesFromResponse() throws Exception {
    String recordOne = "{\"timestamp\":\"2024-02-08T10:00:00Z\",\"type\":\"start\"}\n";
    String recordTwo = "{\"timestamp\":\"2024-02-08T10:00:01Z\",\"type\":\"stop\"}\n";
    byte[] corrupted = new byte[recordOne.length() + 5 + recordTwo.length()];
    System.arraycopy(recordOne.getBytes(StandardCharsets.UTF_8), 0, corrupted, 0, recordOne.length());
    // 5 NUL bytes inserted between two otherwise-valid records
    for (int i = 0; i < 5; i++) {
      corrupted[recordOne.length() + i] = 0;
    }
    System.arraycopy(recordTwo.getBytes(StandardCharsets.UTF_8), 0, corrupted, recordOne.length() + 5,
        recordTwo.length());
    Files.write(Paths.get(logDir, "audit.log"), corrupted);

    StreamingOutput response = apiAuditLogsService.getAuditLogs(LocalDate.now().toString(), LocalDate.now().toString());

    String actualContent = getResponseContent(response);
    assertThat(actualContent).isEqualTo(recordOne + recordTwo);
    assertThat(actualContent).doesNotContain("\0");
  }

  @Test
  public void testGetAuditLogs_HandlesAllNulChunk() throws Exception {
    byte[] onlyNuls = new byte[8192];
    Files.write(Paths.get(logDir, "audit.log"), onlyNuls);

    StreamingOutput response = apiAuditLogsService.getAuditLogs(LocalDate.now().toString(), LocalDate.now().toString());

    assertThat(getResponseContent(response)).isEmpty();
  }

  @Test
  public void testGetAuditLogs_StripsNulBytesFromGzippedArchive() throws Exception {
    String record = "{\"timestamp\":\"2024-02-08T10:00:00Z\",\"type\":\"start\"}\n";
    byte[] corrupted = new byte[record.length() + 4];
    System.arraycopy(record.getBytes(StandardCharsets.UTF_8), 0, corrupted, 0, record.length());
    for (int i = 0; i < 4; i++) {
      corrupted[record.length() + i] = 0;
    }
    File gz = Paths.get(logDir, "audit-2024-02-08.log.gz").toFile();
    try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz.toPath()))) {
      out.write(corrupted);
    }

    StreamingOutput response = apiAuditLogsService.getAuditLogs("2024-02-08", "2024-02-08");

    String actualContent = getResponseContent(response);
    assertThat(actualContent).isEqualTo(record);
    assertThat(actualContent).doesNotContain("\0");
  }

  @Test
  public void testGetAuditLogs_AuditLogNotConfigured() {
    // Set sonatypeWork to a non-existent directory to simulate no log path
    insightConfig.setSonatypeWork("/nonexistent/path/that/does/not/exist");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot get the audit log path.");
  }

  @Test
  public void testGetAuditLogs_StartUtcDateAndEndUtcDateValidations() {
    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("2024-02-04", null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("startUtcDate and endUtcDate must be defined");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("2024-02-04", "")).isInstanceOf(BadRequestException.class)
        .hasMessage("endUtcDate '' is invalid");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs(" ", null)).isInstanceOf(BadRequestException.class)
        .hasMessage("startUtcDate and endUtcDate must be defined");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs(null, "2024-02-04"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("startUtcDate and endUtcDate must be defined");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("", "2024-02-04")).isInstanceOf(BadRequestException.class)
        .hasMessage("startUtcDate '' is invalid");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs(null, " ")).isInstanceOf(BadRequestException.class)
        .hasMessage("startUtcDate and endUtcDate must be defined");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-01"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("startUtcDate must be before endUtcDate");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("2024-02-04", "2024-13-01"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("endUtcDate '2024-13-01' is invalid");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("2024-13-04", "2024-02-01"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("startUtcDate '2024-13-04' is invalid");

    assertThatThrownBy(() -> apiAuditLogsService.getAuditLogs("2024-03-04", LocalDate.now().plusDays(1).toString()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("endUtcDate cannot be in the future");
  }

  private String getResponseContent(StreamingOutput streamingOutput) throws WebApplicationException, IOException {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      streamingOutput.write(output);
      return output.toString("UTF-8");
    }
  }

  private void copyTestResource(String filename) throws IOException {
    String filepath = getClass().getClassLoader().getResource(getClass().getSimpleName() + "/" + filename).getFile();
    Files.copy(new File(filepath).toPath(), Paths.get(logDir, filename));
  }
}

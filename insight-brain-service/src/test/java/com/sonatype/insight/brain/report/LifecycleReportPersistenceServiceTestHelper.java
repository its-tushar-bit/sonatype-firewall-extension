/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public interface LifecycleReportPersistenceServiceTestHelper
{
  /**
   * Methods in implementations of this interface that do not take an application id and scan id use these values
   * as defaults
   */
  String APPLICATION_ID = "app1";

  String SCAN_ID = "scan1";

  String TEST_REPORT_CLASSPATH = "/LifecycleReportPersistenceServiceTest/";

  String DEFAULT_REPORT_NAME = "report";

  /**
   * Save a mock report based on the contents of /LifecycleReportPersistenceServiceTest/report on the classpath
   */
  default void saveMockReport() throws IOException {
    saveMockReport(DEFAULT_REPORT_NAME);
  }

  void saveMockReport(String reportName) throws IOException;

  /**
   * Save a mock report with no files except for a minimal index.html
   */
  default void saveEmptyMockReport() throws IOException {
    saveEmptyMockReport(SCAN_ID);
  }

  void saveEmptyMockReport(String scanId) throws IOException;

  String readFromLocalFiles(String applicationId, String scanId, String name) throws IOException;

  default String readFromLocalFiles(String name) throws IOException {
    return readFromLocalFiles(APPLICATION_ID, SCAN_ID, name);
  }

  String readFromOriginalFiles(String applicationId, String scanId, String name) throws IOException;

  default String readFromOriginalFiles(String name) throws IOException {
    return readFromOriginalFiles(APPLICATION_ID, SCAN_ID, name);
  }

  String readFromAdditionalFiles(String applicationId, String scanId, String name) throws IOException;

  default String readFromAdditionalFiles(String name) throws IOException {
    return readFromAdditionalFiles(APPLICATION_ID, SCAN_ID, name);
  }

  String readPdf(String applicationId, String scanId) throws IOException;

  default String readPdf() throws IOException {
    return readPdf(APPLICATION_ID, SCAN_ID);
  }

  String readVulnerabilitySignatures(String applicationId, String scanId) throws IOException;

  default String readVulnerabilitySignatures() throws IOException {
    return readVulnerabilitySignatures(APPLICATION_ID, SCAN_ID);
  }

  void writeAdditionalFile(String applicationId, String scanId, String name, String content) throws IOException;

  default void writeAdditionalFile(String name, String content) throws IOException {
    writeAdditionalFile(APPLICATION_ID, SCAN_ID, name, content);
  }

  void writeLocalFile(String applicationId, String scanId, String name, String content) throws IOException;

  default void writeLocalFile(String name, String content) throws IOException {
    writeLocalFile(APPLICATION_ID, SCAN_ID, name, content);
  }

  void writePdf(String applicationId, String scanId, String content) throws IOException;

  default void writePdf(String content) throws IOException {
    writePdf(APPLICATION_ID, SCAN_ID, content);
  }

  void writeVulnerabilitySignatures(String applicationId, String scanId, String content) throws IOException;

  default void writeVulnerabilitySignatures(String content) throws IOException {
    writeVulnerabilitySignatures(APPLICATION_ID, SCAN_ID, content);
  }

  /**
   * Attempt to guarantee that an access after this will have a different timestamp from entity.getTime than
   * one before it. How long must be waited in order to accomplish this depends on the backend.
   */
  void waitForNewFileTime() throws InterruptedException;

  default void assertEntityContents(BaseReportEntity entity, String expectedContents) throws IOException {
    try (var inputStream = entity.getInputStream()) {
      assertThat(inputStream).isNotNull();
      byte[] entityContents = inputStream.readAllBytes();
      assertThat(new String(entityContents, StandardCharsets.UTF_8)).isEqualTo(expectedContents);
    }
  }
}

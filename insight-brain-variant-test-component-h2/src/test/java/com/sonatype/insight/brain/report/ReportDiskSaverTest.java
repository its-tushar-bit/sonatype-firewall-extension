/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ComponentH2Test
public class ReportDiskSaverTest
    extends AbstractComponentH2Test
{
  @Inject
  private ReportDiskSaver reportDiskSaver;

  @Test
  public void testMinifyReports() throws IOException {
    // set up: make a copy of the original report folder for testing
    File reportDir = new File(getClass().getResource("/ReportDiskSaverTest/reports").getFile());
    File testReportDir = new File(reportDir.getParentFile(), "tmp-reports");
    FileUtils.copyDirectory(reportDir, testReportDir);

    // when:
    reportDiskSaver.minifyReports(testReportDir);

    // then: artifacts were removed from the first zip file
    File reportZip = new File(
        getClass().getResource("/ReportDiskSaverTest/tmp-reports/report-with-artifacts/report.zip").getFile());
    assertThatReportZipContains(reportZip, "bom.json");
    assertThatReportZipContains(reportZip, "data.json");
    assertThatReportZipDoesNotContain(reportZip, "appcheck.js");
    assertThatReportZipDoesNotContain(reportZip, "orange_arrow.png");

    // and: the second zip file is unchanged
    File reportZip1 = new File(
        getClass().getResource("/ReportDiskSaverTest/reports/report-without-artifacts/report.zip").getFile());
    File reportZip2 = new File(
        getClass().getResource("/ReportDiskSaverTest/tmp-reports/report-without-artifacts/report.zip").getFile());
    assertThat(reportZip1.length()).isEqualTo(reportZip2.length());

    // clean up
    FileUtils.deleteDirectory(testReportDir);
  }

  @Test
  public void testMinifyReports_filesArePreservedOnIOExceptions() throws IOException {
    // set up: make a copy of the original report folder for testing
    File reportDir = new File(getClass().getResource("/ReportDiskSaverTest/reports").getFile());
    File testReportDir = new File(reportDir.getParentFile(), "tmp-reports");
    FileUtils.copyDirectory(reportDir, testReportDir);

    // when:
    try (MockedStatic<FileUtils> fileUtilsMockedStatic = Mockito.mockStatic(FileUtils.class)) {
      fileUtilsMockedStatic.when(() -> FileUtils.copyFile(any(File.class), any(File.class)))
          .thenThrow(IOException.class);

      reportDiskSaver.minifyReports(testReportDir);

      // then: the zip files are unchanged
      File reportZip1 = new File(
          getClass().getResource("/ReportDiskSaverTest/reports/report-with-artifacts/report.zip").getFile());
      File reportZip2 = new File(
          getClass().getResource("/ReportDiskSaverTest/tmp-reports/report-with-artifacts/report.zip").getFile());
      assertThat(reportZip1.length()).isEqualTo(reportZip2.length());

      reportZip1 = new File(
          getClass().getResource("/ReportDiskSaverTest/reports/report-without-artifacts/report.zip").getFile());
      reportZip2 = new File(
          getClass().getResource("/ReportDiskSaverTest/tmp-reports/report-without-artifacts/report.zip").getFile());
      assertThat(reportZip1.length()).isEqualTo(reportZip2.length());
    }

    // clean up
    FileUtils.deleteDirectory(testReportDir);
  }

  private void assertThatReportZipContains(File zipFile, final String thirdPartyFile) throws IOException {
    try (ZipFile y = new ZipFile(zipFile)) {
      assertThat(y.stream().anyMatch(zipEntry -> zipEntry.getName().endsWith(thirdPartyFile))).isTrue();
    }
  }

  private void assertThatReportZipDoesNotContain(File zipFile, final String thirdPartyFile) throws IOException {
    try (ZipFile y = new ZipFile(zipFile)) {
      assertThat(y.stream().anyMatch(zipEntry -> zipEntry.getName().endsWith(thirdPartyFile))).isFalse();
    }
  }
}

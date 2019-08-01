/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.test.InjectedTest;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerTest
    extends InjectedTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(Scanner.class);

  @Inject
  private Scanner scanner;

  @Test
  public void testScan_FingerprintPerformanceLoggingMessage() throws Exception {
    List<File> targets = Collections.singletonList(new File("src/test/resources/ScannerTest/app.ear"));

    scanner.scan(tmpDir.newFile("scan-test.xml.gz"), targets, new Properties());

    assertThat(logOutput).atInfoLevel()
        .containsPattern("Fingerprinting completed in \\d+ seconds for 4 archives, 60 total files");
  }

  @Test
  public void testScan_ReturnResult() throws Exception {
    List<File> targets = Collections.singletonList(new File("src/test/resources/ScannerTest/app.ear"));

    ClientScanResult clientScanResult = scanner.scan(tmpDir.newFile("scan-test.xml.gz"), targets, new Properties());

    assertThat(clientScanResult).isNotNull();
    assertThat(clientScanResult.getScanFile()).isNotNull();
    assertThat(clientScanResult.hasThirdPartyScanContent()).isFalse();
  }
}

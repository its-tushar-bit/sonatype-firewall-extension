/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.mock.twistlock.TwistlockMockServerRule;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.io.ScanReader;

import com.google.inject.Binder;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TwistlockPolicyEvaluatorTest
    extends InjectedTest
{
  private static final String DEFAULT_TWISTLOCK_RESULTS_PATH = "/api/v1/scan/scan-2016-10-11T18:38:13.773Z.tar.gz";

  private static final String DEFAULT_TWISTLOCK_RESULTS_URL =
      "http://localhost:${twistlockServerPort}" + DEFAULT_TWISTLOCK_RESULTS_PATH;

  private static final String DEFAULT_TWISTLOCK_SCANNER_OUTPUT = "\nScan completed. Results at: "
      + DEFAULT_TWISTLOCK_RESULTS_URL + "\n";

  @Rule
  public TwistlockMockServerRule twistlockMockServer = new TwistlockMockServerRule();

  public TwistlockScanner spyTwistlockScanner;

  @Inject
  private TwistlockPolicyEvaluator evaluator;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    spyTwistlockScanner = spy(new TwistlockScanner()
    {
      // Need to bypass the actual run of the twistlock scanner command because the twistlock scanner doesn't exist for
      // these tests.
      @Override
      String runTwistlockScannerCommand(List<String> twistlockScannerCommand) {
        return DEFAULT_TWISTLOCK_SCANNER_OUTPUT.replace("${twistlockServerPort}",
            String.valueOf(twistlockMockServer.getHttpPort()));
      }
    });
    binder.bind(TwistlockScanner.class).toInstance(spyTwistlockScanner);
  }

  @Test
  public void testScan() throws Exception {
    TwistlockParameters params = new TwistlockParameters("--twistlock-scanner-executable",
        "twistlock-2-2-87/twistcli", "--twistlock-console-url", "https://localhost:8083",
        "--twistlock-console-username", "admin", "--twistlock-console-password", "1Twistlock$", "02c0ca9581ac");

    twistlockMockServer.setResponseForURI(DEFAULT_TWISTLOCK_RESULTS_PATH,
        getClass().getResource("/TwistlockPolicyEvaluatorTest/scan-results.tar.gz"), 200);

    File scanFile = evaluator.scan(params, new ProprietaryConfig());
    // scanFileZip.close() closes all InputStreams retrieved from this archive
    try (ZipFile scanFileZip = new ZipFile(scanFile)) {
      // Verify the Twistlock scan in the scan zip file
      ZipEntry entry = scanFileZip.getEntry("twistlockScanFile.tar.gz");
      try (InputStream expected = getClass().getResourceAsStream("/TwistlockPolicyEvaluatorTest/scan-results.tar.gz")) {
        assertThat(IOUtil.contentEquals(expected, scanFileZip.getInputStream(entry))).isTrue();
      }

      // Verify the Sonatype scan in the scan zip file
      entry = scanFileZip.getEntry("scan.xml.gz");
      ScanReader scanReader = new ScanReader();
      Scan scan = scanReader.read(new GZIPInputStream(scanFileZip.getInputStream(entry)));
      assertThat(scan.getSummary().getStartTime()).isNotNull();
      assertThat(scan.getSummary().getEndTime()).isNotNull();
      assertThat(scan.getSummary().getClientInfo()).isNotEmpty();
    }
    finally {
      scanFile.delete();
    }
  }

  @Test
  public void testScan_TwistlockScannerParameters() throws Exception {
    TwistlockParameters twistlockParameters = new TwistlockParameters( //
        "--twistlock-scanner-executable", "twistlock-2-2-87/twistcli", //
        "--twistlock-console-url", "https://localhost:8083", //
        "--twistlock-console-username", "admin", "--twistlock-console-password", "1Twistlock$", //
        "02c0ca9581ac");

    List<String> expectedParameters = Arrays.asList("twistlock-2-2-87/twistcli", //
        "images", "scan", //
        "--address", "https://localhost:8083", //
        "--user", "admin", //
        "--include-files", "--include-package-files", //
        "--hash", "sha1", //
        "--upload", //
        "--password", "1Twistlock$", //
        "02c0ca9581ac");

    testScan_TwistlockScannerParameters(twistlockParameters, expectedParameters);
  }

  private void testScan_TwistlockScannerParameters(TwistlockParameters twistlockParameters,
                                                   List<String> expectedParameters) throws Exception
  {
    twistlockMockServer.setResponseForURI(DEFAULT_TWISTLOCK_RESULTS_PATH,
        getClass().getResource("/TwistlockPolicyEvaluatorTest/scan-results.tar.gz"), 200);

    evaluator.scan(twistlockParameters, new ProprietaryConfig());
    
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> argCaptor = ArgumentCaptor.forClass(List.class);
    verify(spyTwistlockScanner).runTwistlockScannerCommand(argCaptor.capture());
    assertThat(argCaptor.getValue()).isEqualTo(expectedParameters);
  }

  @Test
  public void testExtractScanResultUrl_Success() {
    String scannerOutput = DEFAULT_TWISTLOCK_SCANNER_OUTPUT;
    String scanResultsUrl = evaluator.extractScanResultUrl(scannerOutput);
    assertThat(scanResultsUrl).isEqualTo(DEFAULT_TWISTLOCK_RESULTS_URL);
  }

  @Test
  public void testExtractScanResultUrl_Failure() {
    String scannerOutput = "Can't win them all!";
    assertThatThrownBy(() -> {
      evaluator.extractScanResultUrl(scannerOutput);
    }).isInstanceOf(RuntimeException.class).hasMessage("Twistlock scanner failed with:\nCan't win them all!");
  }
}

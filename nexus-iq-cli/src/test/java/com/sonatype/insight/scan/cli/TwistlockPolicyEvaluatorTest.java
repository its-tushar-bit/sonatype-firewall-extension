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
import com.sonatype.insight.mock.twistlock.PortAllocator;
import com.sonatype.insight.mock.twistlock.TwistlockMockServerRule;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.io.DefaultScanReader;
import com.sonatype.insight.scan.model.io.ScanReader;

import com.google.inject.Binder;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TwistlockPolicyEvaluatorTest
    extends InjectedTest
{
  private static final String DEFAULT_TWISTLOCK_RESULTS_URL = "http://localhost:${twistlockServerPort}/api/v1/scan/scan-2016-10-11T18:38:13.773Z.tar.gz";

  private static final String DEFAULT_TWISTLOCK_SCANNER_OUTPUT = "\n" + //
      "Scan completed. Image \"02c0ca9581ac\"" + TwistlockPolicyEvaluator.TWISTLOCK_SCAN_SUCCESS_MARKER + ";\n" + //
      DEFAULT_TWISTLOCK_RESULTS_URL + "\n";

  private int twistlockServerPort = PortAllocator.findFreePort(8083);

  @Rule
  public TwistlockMockServerRule twistlockMockServer = new TwistlockMockServerRule(twistlockServerPort);

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
        return DEFAULT_TWISTLOCK_SCANNER_OUTPUT.replace("${twistlockServerPort}", String.valueOf(twistlockServerPort));
      }
    });
    binder.bind(TwistlockScanner.class).toInstance(spyTwistlockScanner);
  }

  @Test
  public void testScan() throws Exception {
    TwistlockParameters params = new TwistlockParameters("--twistlock-scanner-executable",
        "twistlock-1-5-47/twistlock-scanner", "--twistlock-console-url", "https://localhost:8083",
        "--twistlock-console-username", "admin", "--twistlock-console-password", "1Twistlock$", "02c0ca9581ac");

    twistlockMockServer.setResponseForURI(
        DEFAULT_TWISTLOCK_RESULTS_URL.replace("${twistlockServerPort}", String.valueOf(twistlockServerPort)),
        getClass().getResource("/TwistlockPolicyEvaluatorTest/scan-results.tar.gz"), 200);

    File scanFile = evaluator.scan(params, new ProprietaryConfig());
    ZipFile scanFileZip = new ZipFile(scanFile);
    try {
      // Verify the Twistlock scan in the scan zip file
      ZipEntry entry = scanFileZip.getEntry("twistlockScanFile.tar.gz");
      InputStream expected = getClass().getResource("/TwistlockPolicyEvaluatorTest/scan-results.tar.gz").openStream();
      try {
        assertThat(IOUtil.contentEquals(expected, scanFileZip.getInputStream(entry)), is(true));
      }
      finally {
        expected.close();
      }

      // Verify the Sonatype scan in the scan zip file
      entry = scanFileZip.getEntry("scan.xml.gz");
      ScanReader scanReader = new DefaultScanReader();
      Scan scan = scanReader.read(new GZIPInputStream(scanFileZip.getInputStream(entry)));
      assertThat(scan.getSummary().getStartTime(), notNullValue());
      assertThat(scan.getSummary().getEndTime(), notNullValue());
      assertThat(scan.getSummary().getClientInfo().size(), greaterThan(0));
    }
    finally {
      scanFileZip.close(); // closes all InputStreams retrieved from this archive
      scanFile.delete();
    }
  }

  @Test
  public void testScan_TwistlockScannerParameters() throws Exception {
    TwistlockParameters twistlockParameters = new TwistlockParameters( //
        "--twistlock-scanner-executable", "twistlock-1-5-47/twistlock-scanner", //
        "--twistlock-console-url", "https://localhost:8083", //
        "--twistlock-console-username", "admin", "--twistlock-console-password", "1Twistlock$", //
        "02c0ca9581ac");

    List<String> expectedParameters = Arrays.asList("twistlock-1-5-47/twistlock-scanner", "-c",
        "https://localhost:8083", "-u", "admin", "-i", "02c0ca9581ac", "--include-files", "--include-package-files",
        "--hash-method", "sha1", "-p", "1Twistlock$");

    testScan_TwistlockScannerParameters(twistlockParameters, expectedParameters);
  }

  @Test
  public void testScan_TwistlockScannerParameters_tlsverify() throws Exception {
    TwistlockParameters twistlockParameters = new TwistlockParameters( //
        "--twistlock-scanner-executable", "twistlock-1-5-47/twistlock-scanner", //
        "--twistlock-console-url", "https://localhost:8083", //
        "--twistlock-console-username", "admin", "--twistlock-console-password", "1Twistlock$", //
        "--twistlock-tlsverify", "false", //
        "02c0ca9581ac");

    List<String> expectedParameters = Arrays.asList("twistlock-1-5-47/twistlock-scanner", "-c",
        "https://localhost:8083", "-u", "admin", "-i", "02c0ca9581ac", "--include-files", "--include-package-files",
        "--hash-method", "sha1", "--tlsverify=false", "-p", "1Twistlock$");

    testScan_TwistlockScannerParameters(twistlockParameters, expectedParameters);
  }

  private void testScan_TwistlockScannerParameters(TwistlockParameters twistlockParameters,
                                                   List<String> expectedParameters) throws Exception
  {
    twistlockMockServer.setResponseForURI(
        DEFAULT_TWISTLOCK_RESULTS_URL.replace("${twistlockServerPort}", String.valueOf(twistlockServerPort)),
        getClass().getResource("/TwistlockPolicyEvaluatorTest/scan-results.tar.gz"), 200);

    evaluator.scan(twistlockParameters, new ProprietaryConfig());
    
    @SuppressWarnings("deprecation")
    ArgumentCaptor<List<String>> argCaptor = new ArgumentCaptor<List<String>>();
    verify(spyTwistlockScanner).runTwistlockScannerCommand(argCaptor.capture());
    assertThat(argCaptor.getValue(), is(expectedParameters));
  }

  @Test
  public void testParseTwistlockScannerOutput_Success() {
    String scannerOutput = DEFAULT_TWISTLOCK_SCANNER_OUTPUT;
    String scanResultsUrl = evaluator.parseTwistlockScannerOutput(scannerOutput);
    assertThat(scanResultsUrl, is(DEFAULT_TWISTLOCK_RESULTS_URL));
  }

  @Test
  public void testParseTwistlockScannerOutput_Failure() {
    String scannerOutput = "Can't win them all!";
    try {
      evaluator.parseTwistlockScannerOutput(scannerOutput);
      fail("Expected exception");
    }
    catch (RuntimeException expected) {
      assertThat(expected.getMessage(), is("Twistlock scanner failed with:\nCan't win them all!"));
    }
  }
}

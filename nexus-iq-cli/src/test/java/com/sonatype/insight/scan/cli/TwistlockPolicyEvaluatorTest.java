/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.mock.PortAllocator;
import com.sonatype.insight.mock.TwistlockMockServerRule;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.io.DefaultScanReader;
import com.sonatype.insight.scan.model.io.ScanReader;

import com.google.inject.Binder;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TwistlockPolicyEvaluatorTest
    extends InjectedTest
{
  private static final String DEFAULT_TWISTLOCK_RESULTS_URL = "http://localhost:${twistlockServerPort}/api/v1/scan/scan-2016-10-11T18:38:13.773Z.tar.gz";

  private static final String DEFAULT_TWISTLOCK_SCANNER_OUTPUT = "\n" + //
      "Scan completed. Image \"02c0ca9581ac\" evaluated successfully, Results at;\n" + //
      DEFAULT_TWISTLOCK_RESULTS_URL + "\n";

  private int twistlockServerPort = PortAllocator.findFreePort(8083);

  @Rule
  public TwistlockMockServerRule twistlockMockServer = new TwistlockMockServerRule(twistlockServerPort);

  @Mock
  public TwistlockScanner mockTwistlockScanner;

  @Inject
  private TwistlockPolicyEvaluator evaluator;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(TwistlockScanner.class).toInstance(mockTwistlockScanner);
  }

  @Test
  public void testScan() throws Exception {
    when(mockTwistlockScanner.scan(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(
        DEFAULT_TWISTLOCK_SCANNER_OUTPUT.replace("${twistlockServerPort}", String.valueOf(twistlockServerPort)));
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

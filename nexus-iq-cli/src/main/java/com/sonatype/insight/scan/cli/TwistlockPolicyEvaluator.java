/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanFileNames;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.24
 */
@Named
public class TwistlockPolicyEvaluator
    extends PolicyEvaluator<TwistlockParameters>
{
  private static final Logger log = LoggerFactory.getLogger(TwistlockPolicyEvaluator.class);

  static final String TWISTLOCK_SCAN_SUCCESS_MARKER = "Scan completed. Results at: ";

  private final TwistlockScanner twistlockScanner;

  private final ScanWriterFactory writerFactory;

  private final ClientScanner clientScanner;

  @Inject
  public TwistlockPolicyEvaluator(TwistlockScanner twistlockScanner,
                                  Scanner scanner,
                                  RestClientFactory restClientFactory,
                                  ScanWriterFactory writerFactory,
                                  ClientScanner clientScanner)
  {
    super(scanner, restClientFactory);
    this.twistlockScanner = twistlockScanner;
    this.writerFactory = writerFactory;
    this.clientScanner = clientScanner;
  }

  @Override
  protected ClientScanResult scan(TwistlockParameters params,
                                  ProprietaryConfig proprietaryConfig,
                                  RestClient restClient) throws ExitException
  {
    params.getOutputDirectory().mkdirs();

    Scan scan = createScan(getScanConfiguration(params, proprietaryConfig));

    scan.getSummary().setStartTime();
    File twistlockScanFile = runTwistlockScanner(params);
    scan.getSummary().setEndTime();

    File scanFile = writeScanFile(params.getOutputDirectory(), twistlockScanFile, scan);

    return new ClientScanResult(scanFile, scan.hasThirdPartyScanContent());
  }

  /**
   * Writes the final scan file, which contains the Twistlock scan (twistlockScanFile.tar.gz) and the Sonatype scan
   * (scan.xml.gz).
   */
  private File writeScanFile(File targetDir, File twistlockScanFile, Scan scan) {
    try {
      File scanFile = File.createTempFile("scan-", ".zip", targetDir);
      try (ZipOutputStream scanFileStream = new ZipOutputStream(new FileOutputStream(scanFile))) {
        ZipEntry zipEntry = new ZipEntry(ScanFileNames.TWISTLOCK_SCAN_FILENAME);
        scanFileStream.putNextEntry(zipEntry);
        FileUtils.copyFile(twistlockScanFile, scanFileStream);
        scanFileStream.closeEntry();

        zipEntry = new ZipEntry(ScanFileNames.SONATYPE_SCAN_FILENAME);
        scanFileStream.putNextEntry(zipEntry);
        writeScan(scan, new OutputStreamWriter(new GZIPOutputStream(scanFileStream), "UTF-8"));
      }

      return scanFile;
    }
    catch (IOException e) {
      throw new UncheckedIOException("Error writing scan file: " + e.getMessage(), e);
    }
  }

  private File runTwistlockScanner(TwistlockParameters params) {
    String twistlockScannerExecutable = params.getTwistlockScannerExecutable();
    String twistlockConsoleUrl = params.getTwistlockConsoleUrl();
    String twistlockConsoleUsername = params.getTwistlockConsoleUsername();
    String twistlockConsolePassword = params.getTwistlockConsolePassword();
    String imageId = params.getScanTargets().get(0);

    String scannerOutput = twistlockScanner.scan(twistlockScannerExecutable, imageId, twistlockConsoleUrl,
        twistlockConsoleUsername, twistlockConsolePassword);
    String scanResultUrl = extractScanResultUrl(scannerOutput);
    log.info("Twistlock scan results at: {}", scanResultUrl);

    Configuration config = newTwistlockHttpClientConfig(scanResultUrl, twistlockConsoleUsername,
        twistlockConsolePassword);
    TwistlockHttpClient twistlockHttpClient = new TwistlockHttpClient(config);
    File scanResultsFile = new File(params.getOutputDirectory(), getTwistlockScanFilename(scanResultUrl));
    twistlockHttpClient.downloadScanResults(scanResultsFile);
    log.info("Saved Twistlock scan results to {}", scanResultsFile.getAbsolutePath());

    return scanResultsFile;
  }

  private Scan createScan(Properties config) {
    Scan scan = new Scan();
    scan.setConfiguration(new ScanConfiguration(config));
    clientScanner.scan(new ClientScanRequest(scan));
    return scan;
  }

  private void writeScan(Scan scan, Writer writer) throws IOException {
    try (ScanWriter scanWriter = writerFactory.newWriter(writer)) {
      scanWriter.openScan(scan);
      scanWriter.writeConfiguration(scan.getConfiguration());
      clientScanner.scan(new ClientScanRequest(scan));
      scanWriter.writeSummary(scan.getSummary());
      scanWriter.closeScan();
    }
  }

  private String getTwistlockScanFilename(String scanResultUrl) {
    try {
      URL url = new URL(scanResultUrl);
      File path = new File(url.getPath());
      return path.getName().replace(':', '_');
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private Configuration newTwistlockHttpClientConfig(String url, String username, String password) {
    Configuration config = new Configuration();
    config.setServerUrl(url);
    SimpleAuthentication auth = new SimpleAuthentication();
    auth.setUsername(username);
    auth.setPassword(password);
    config.setServerAuth(auth);
    return config;
  }

  String extractScanResultUrl(String scannerOutput) {
    for (String scannerOutputLine : scannerOutput.split("\n")) {
      if (scannerOutputLine.startsWith(TWISTLOCK_SCAN_SUCCESS_MARKER)) {
        return scannerOutputLine.substring(TWISTLOCK_SCAN_SUCCESS_MARKER.length());
      }
    }

    throw new RuntimeException("Twistlock scanner failed with:\n" + scannerOutput);
  }

  @Override
  protected void validateScanTargets(TwistlockParameters params, RestClient restClient) throws ExitException {
    if (params.getScanTargets().isEmpty()) {
      String message = "The ID of the image to scan was not specified.";
      log.error(message);
      throw new ExitException(1, message);
    }
  }

  @Override
  protected ClientScanType getClientScanType() {
    return ClientScanType.TWISTLOCK;
  }

  @Override
  protected ProprietaryConfig getProprietaryConfiguration(TwistlockParameters params, RestClient restClient)
      throws ExitException
  {
    // For Docker images, the proprietary components are determined on the server.
    return new ProprietaryConfig();
  }
}

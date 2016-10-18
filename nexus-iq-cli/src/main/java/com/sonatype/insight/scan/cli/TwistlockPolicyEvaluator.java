/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

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

  private final TwistlockScanner twistlockScanner;

  @Inject
  public TwistlockPolicyEvaluator(TwistlockScanner twistlockScanner,
                                  Scanner scanner,
                                  RestClientFactory restClientFactory)
  {
    super(scanner, restClientFactory);
    this.twistlockScanner = twistlockScanner;
  }

  @Override
  protected File scan(TwistlockParameters params, ProprietaryConfig proprietaryConfig) throws ExitException {
    String twistlockScannerExecutable = params.getTwistlockScannerExecutable();
    String twistlockConsoleUrl = params.getTwistlockConsoleUrl();
    String twistlockConsoleUsername = params.getTwistlockConsoleUsername();
    String twistlockConsolePassword = params.getTwistlockConsolePassword();
    String imageId = params.getScanTargets().get(0);

    String scannerOutput = twistlockScanner.scan(twistlockScannerExecutable, imageId, twistlockConsoleUrl,
        twistlockConsoleUsername, twistlockConsolePassword);
    String scanResultUrl = parseTwistlockScannerOutput(scannerOutput);
    log.info("Twistlock scan results at: {}", scanResultUrl);

    Configuration config = newTwistlockHttpClientConfig(scanResultUrl, twistlockConsoleUsername,
        twistlockConsolePassword);
    TwistlockHttpClient twistlockHttpClient = new TwistlockHttpClient(config);
    File scanResultsFile = new File(getScanFilename(scanResultUrl).replace(':', '_'));
    twistlockHttpClient.downloadScanResults(scanResultsFile);
    log.info("Saved Twistlock scan results to {}", scanResultsFile.getAbsolutePath());

    return scanResultsFile;
  }

  private String getScanFilename(String scanResultUrl) {
    try {
      URL url = new URL(scanResultUrl);
      File path = new File(url.getPath());
      return path.getName();
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

  String parseTwistlockScannerOutput(String scannerOutput) {
    boolean wasScanSuccessful = false;
    for (String scannerOutputLine : scannerOutput.split("\n")) {
      if (wasScanSuccessful) {
        return scannerOutputLine;
      }
      if (scannerOutputLine.contains(" evaluated successfully, Results at")) {
        wasScanSuccessful = true;
      }
    }

    throw new RuntimeException("Twistlock scanner failed with:\n" + scannerOutput);
  }

  @Override
  protected void validateScanTargets(List<String> scanTargets) throws ExitException {
    if (scanTargets.isEmpty()) {
      String message = "The ID of the image to scan was not specified.";
      log.error(message);
      throw new ExitException(1, message);
    }
  }
}

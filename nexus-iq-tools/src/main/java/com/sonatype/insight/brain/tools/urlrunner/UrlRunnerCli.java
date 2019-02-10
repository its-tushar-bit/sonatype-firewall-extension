/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import com.sonatype.insight.brain.tools.common.PerfTestConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlRunnerCli
{
  private static final Logger log = LoggerFactory.getLogger(UrlRunnerCli.class);

  public static void main(String[] args) {
    try {
      new UrlRunnerCli().run(args);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  //default visibility for testing
  void run(String[] args) throws Exception {
    Parameters params = new Parameters(args);
    if (params.getError() != null) {
      params.printUsage();
      log.error("Actual arguments were: {}", Arrays.asList(params.getArgs()));
      System.exit(1);
    }
    if (params.isHelp()) {
      params.printUsage();
      return;
    }
    run(params);
  }

  private void run(Parameters params) throws Exception {
    UrlRunner urlRunner = new UrlRunner();
    urlRunner.run(getInputObject(params.getInputFile()), params.getServer(), params.getUsername(), params.getPassword(),
        this::printStats, params.getAdminServer(), params.getProxy());
  }

  private static PerfTestConfig getInputObject(File file) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(file, PerfTestConfig.class);
  }

  // default for testing
  void printStats(Stats stats) {
    printStats(stats, log);
  }

  // public for testing
  public void printStats(Stats stats, Logger log) {
    try {
      log.info("-------------");
      log.info("URL : {}", stats.getUrl());
      if (stats.getRequestPayload() != null) {
        log.info("Request Payload: {}", stats.getRequestPayload());
      }
      log.info("Response Status: {} {}", stats.getStatusLine().getStatusCode(),
          stats.getStatusLine().getReasonPhrase());
      log.info("Response Time: {}", stats.getResponseTime());

      if (StringUtils.isNotEmpty(stats.getResponseBody())) {
        log.info("Size: {}", stats.getResponseBody().getBytes().length);
        log.debug("Response body: {}", stats.getResponseBody());
        log.info("MD5 of response: {}", getMD5(stats.getResponseBody()));
      }

      if (stats.getMetricsReport() != null) {
        stats.getMetricsReport().printMetrics();
      }
    }
    catch (Exception e) {
      log.info("Error printing stats for: {} see debug log for details", stats.getUrl());
      log.debug("Error in printing stats", e);
    }
  }

  private static String getMD5(String input) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    digest.update(input.getBytes(StandardCharsets.UTF_8));
    byte[] keySum = digest.digest();
    BigInteger bigInt = new BigInteger(1, keySum);
    return bigInt.toString(16);
  }
}

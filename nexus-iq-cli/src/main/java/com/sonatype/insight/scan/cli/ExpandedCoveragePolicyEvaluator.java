/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ExpandedCoverage;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpResponseException;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.Engine.Mode;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.utils.Settings;
import org.owasp.dependencycheck.utils.Settings.KEYS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.34
 */
@Named
public class ExpandedCoveragePolicyEvaluator
    extends PolicyEvaluator
{
  @VisibleForTesting
  static final String EXPANDED_COVERAGE_SCAN_DISCLAIMER = "                         PERFORMING EXPANDED COVERAGE SCAN";

  private static final Logger log = LoggerFactory.getLogger(ExpandedCoveragePolicyEvaluator.class);

  private final ClientScanner clientScanner;

  private final ScanWriterFactory writerFactory;

  @Inject
  public ExpandedCoveragePolicyEvaluator(Scanner scanner,
                                         RestClientFactory restClientFactory,
                                         ClientScanner clientScanner,
                                         ScanWriterFactory writerFactory)
  {
    super(scanner, restClientFactory);
    this.clientScanner = clientScanner;
    this.writerFactory = writerFactory;
  }

  @Override
  protected ClientScanResult scan(Parameters params,
                                  ProprietaryConfig proprietaryConfig,
                                  RestClient restClient) throws ExitException
  {
    log.info("");
    log.info("*********************************************************************************************");
    log.info(EXPANDED_COVERAGE_SCAN_DISCLAIMER);
    log.info("*********************************************************************************************");
    log.info("");
    
    try (Engine engine = newExpandedCoverageEngine()) {
      Files.createDirectories(params.getOutputDirectory().toPath());

      File scanFile = File.createTempFile("scan-", ".xml.gz", params.getOutputDirectory());

      Scan scan = new Scan();
      scan.setConfiguration(new ScanConfiguration(getScanConfiguration(params, proprietaryConfig)));
      try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
        writer.openScan(scan);
        writer.writeConfiguration(scan.getConfiguration());
        scan.getSummary().setStartTime();
        clientScanner.scan(new ClientScanRequest(scan));

        engine.scan(params.getScanTargets().toArray(new String[params.getScanTargets().size()]));
        try {
          engine.analyzeDependencies();
        }
        catch (ExceptionCollection e) {
          if (e.isFatal()) {
            throw e;
          }
          log.warn(e.getMessage(), e);
        }
        List<Dependency> xcItems = Arrays.asList(engine.getDependencies());
        xcItems.forEach(this::fixCMakeAnalyzerDisplayName);
        log.info("Found {} items.", xcItems.size());

        ExpandedCoverage expandedCoverage = new ExpandedCoverage();
        expandedCoverage.setVersion(getExpandedCoverageVersion());
        expandedCoverage.setDependenciesJson(getObjectMapper().writeValueAsString(xcItems));
        writer.writeExpandedCoverage(expandedCoverage);

        scan.getSummary().setEndTime();
        writer.writeSummary(scan.getSummary());
        writer.closeScan();
      }
      return new ClientScanResult(scanFile, scan.hasThirdPartyScanContent());
    }
    catch (Exception e) {
      log.error("The scan could not be performed: " + e.getMessage(), e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  // Temporary fix for CMakeAnalyzer dependency regression showing "CMakeLists.txt" instead of name & version in v3.3.1
  @VisibleForTesting
  void fixCMakeAnalyzerDisplayName(Dependency dependency) {
    String displayFileName = dependency.getDisplayFileName();
    if (displayFileName == null) {
      return;
    }
    if (displayFileName.matches("CMakeLists\\.txt|.*\\.cmake")) {
      if (dependency.getName() == null) {
        return;
      }
      if (dependency.getVersion() == null) {
        dependency.setDisplayFileName(dependency.getName());
        return;
      }
      dependency.setDisplayFileName(dependency.getName() + ":" + dependency.getVersion());
    }
  }

  Engine newExpandedCoverageEngine() {
    return new Engine(Mode.EVIDENCE_COLLECTION, getExpandedCoverageConfiguration());
  }

  /**
   * Policies are not evaluated for expanded coverage scans.
   */
  @Override
  protected void evaluatePolicy(Parameters params,
                                RestClient restClient,
                                ClientScanResult clientScanResult,
                                ClientScanType clientScanType) throws ExitException
  {

    log.info("Submitting scan to the IQ Server...");
    ScanReceipt scanReceipt;
    try {
      scanReceipt = restClient.uploadScan(params.getApplicationId(), clientScanResult.getScanFile(), clientScanType);
      log.info("Assigned scan ID {}", scanReceipt.getScanId());
    }
    catch (HttpResponseException e) {
      log.error("The scan could not be submitted to the IQ Server: {} ({})", e.getMessage(), e.getStatusCode());
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (IOException e) {
      log.error("The scan could not be submitted to the IQ Server", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }

    log.info("Awaiting expanded coverage report (ETA {}s)...", scanReceipt.getTimeToReport());
    try {
      scanReceipt.waitForReport();
      restClient.prepareExpandedCoverageReport(params.getApplicationId(), scanReceipt.getScanId());
    }
    catch (InterruptedException e) {
      log.error("The process was interrupted");
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (IOException e) {
      log.error("The server failed to prepare the report for application ID {}.", params.getApplicationId(), e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }

    String reportUrl = scanReceipt.resolveReportUrl(params.getServerUrl());

    log.info("*********************************************************************************************");
    log.info("Stage: {}", params.getStage().getStageTypeId());
    log.info("The expanded coverage report can be viewed online at {}", reportUrl);
    log.info("*********************************************************************************************");
  }

  Settings getExpandedCoverageConfiguration() {
    Settings settings = new Settings();
    // Enable the experimental and retired analyzers.
    settings.setBoolean(KEYS.ANALYZER_EXPERIMENTAL_ENABLED, true);
    settings.setBoolean(KEYS.ANALYZER_RETIRED_ENABLED, true);
    // Disable analyzers that connect to external resources.
    settings.setBoolean(Settings.KEYS.ANALYZER_CENTRAL_ENABLED, false);
    settings.setBoolean(Settings.KEYS.ANALYZER_ARTIFACTORY_ENABLED, false);
    settings.setBoolean(Settings.KEYS.ANALYZER_NEXUS_ENABLED, false);
    settings.setBoolean(Settings.KEYS.ANALYZER_NSP_PACKAGE_ENABLED, false);
    settings.setBoolean(Settings.KEYS.ANALYZER_BUNDLE_AUDIT_ENABLED, false);
    settings.setBoolean(Settings.KEYS.ANALYZER_RETIREJS_ENABLED, false);
    return settings;
  }

  private String getExpandedCoverageVersion() throws IOException {
    Properties properties = new Properties();
    try (InputStream input = getClass().getResourceAsStream("expanded-coverage.properties")) {
      properties.load(input);
    }
    return properties.getProperty("dependency-check-version", "unknown");
  }

  private ObjectMapper getObjectMapper() {
    // IMPORTANT:
    // The json serialization configuration used here MUST match the json de-serialization on the server side.
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(objectMapper.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE).withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
    objectMapper.setSerializationInclusion(Include.NON_NULL);
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    return objectMapper;
  }

  @Override
  protected ClientScanType getClientScanType() {
    return ClientScanType.EXPANDED_COVERAGE;
  }
}

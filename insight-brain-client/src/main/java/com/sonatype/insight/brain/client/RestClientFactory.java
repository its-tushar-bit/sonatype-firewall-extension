/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.model.ClientScanType;

/**
 * @since 1.12.1
 */
@Named
public class RestClientFactory
{
  public RestClient newRestCIClient(Configuration config) {
    return new RestCIClient(config);
  }

  public RestClient newRestCLIClient(Configuration config) {
    return new RestCLIClient(config);
  }

  /**
   * @since 1.19.0
   */
  public static class RestCLIClient
      extends RestClient
  {
    RestCLIClient(Configuration config) {
      super(config);
    }

    @Override
    public ScanReceipt uploadScan(String appId, File scanFile, ClientScanType clientScanType) throws IOException {
      return new ScanClient(config, appId).uploadCLIScan(scanFile, clientScanType);
    }
  }

  /**
   * @since 1.19.0
   */
  public static class RestCIClient
      extends RestClient
  {
    RestCIClient(final Configuration config) {
      super(config);
    }

    @Override
    public ScanReceipt uploadScan(String appId, File scanFile, ClientScanType clientScanType) throws IOException {
      return new ScanClient(config, appId).uploadCIScan(scanFile);
    }
  }

  public abstract static class RestClient
  {
    protected final Configuration config;

    RestClient(Configuration config) {
      this.config = config;
    }

    public abstract ScanReceipt uploadScan(String appId, File scanFile, ClientScanType clientScanType)
        throws IOException;

    /**
     * @since 1.45.0
     */
    public boolean verifyOrCreateApplication(String applicationPublicId) throws IOException {
      return new ConfigurationClient(config).verifyOrCreateApplication(applicationPublicId);
    }

    /**
     * Get the proprietary configuration used for an application evaluation.
     * 
     * @since 1.22.0
     */
    public ProprietaryConfig getProprietaryConfigForApplicationEvaluation(String applicationPublicId)
        throws IOException
    {
      return new ConfigurationClient(config).getProprietaryConfigForApplicationEvaluation(applicationPublicId);
    }

    /**
     * Get the proprietary configuration used for a component evaluation.
     * 
     * @since 1.22.0
     */
    public ProprietaryConfig getProprietaryConfigForComponentEvaluation(String applicationPublicId)
        throws IOException
    {
      return new ConfigurationClient(config).getProprietaryConfigForComponentEvaluation(applicationPublicId);
    }

    public PolicyEvaluationResult evaluatePolicy(String appId, String scanId, String stageId) throws IOException {
      return new PolicyClient(config, appId).evaluate(scanId, new Stage(stageId));
    }

    public void saveReportBundle(String appId, String scanId, File bundleFile) throws IOException {
      new ReportClient(config, appId, scanId).downloadBundle(bundleFile);
    }

    /**
     * Prepares the report for an expanded coverage scan to be available when the customer loads it in a browser.
     * It waits for the report to become available on the HDS.
     * 
     * @since 1.37
     */
    public void prepareExpandedCoverageReport(String appId, String scanId) throws IOException {
      new ReportClient(config, appId, scanId).prepareExpandedCoverageReport();
    }

    public void saveResults(String appId, File resultFile, ScanReceipt receipt) throws IOException {
      new ScanClient(config, appId).saveResultData(resultFile, receipt);
    }

    /**
     * @since 1.50
     */
    public void validateServerVersion(String minimalServerVersionRequiredAsString) throws IOException {
      new ConfigurationClient(config).validateServerVersion(minimalServerVersionRequiredAsString);
    }
  }
}

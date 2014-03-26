/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.PolicyClient;
import com.sonatype.insight.brain.client.ReportClient;
import com.sonatype.insight.brain.client.ScanClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

/**
 * @since 1.10
 */
@Named
public class RestClientFactory
{

  public RestClient newRestClient(Configuration config) {
    return new RestClient(config);
  }

  public static class RestClient
  {

    private final Configuration config;

    RestClient(Configuration config) {
      this.config = config;
    }

    public Map<String, String> getApplications() throws IOException {
      return new ConfigurationClient(config).getApplicationIdNameMap();
    }

    public ProprietaryConfig getProprietaryConfiguration() throws IOException {
      return new ConfigurationClient(config).getProprietaryConfiguration();
    }

    public ScanReceipt uploadScan(String appId, File scanFile) throws IOException {
      return new ScanClient(config, appId).uploadCiScan(scanFile);
    }

    public PolicyEvaluationResult evaluatePolicy(String appId, String scanId, String stageId) throws IOException {
      return new PolicyClient(config, appId).evaluate(scanId, new Stage(stageId));
    }

    public void saveReportBundle(String appId, String scanId, File bundleFile) throws IOException {
      new ReportClient(config, appId, scanId).downloadBundle(bundleFile);
    }

    public void validateAuthentication() throws IOException {
      new ConfigurationClient(config).validateAuthentication();
    }
  }
}

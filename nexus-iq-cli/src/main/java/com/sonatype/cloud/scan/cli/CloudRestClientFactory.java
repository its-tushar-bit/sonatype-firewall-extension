/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.ScanClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;

/**
 * Cloud implementation of {@link RestClientFactory} to allow creation of specialized {@link RestClient} that can handle
 * the evaluation of a policy via a {@link CloudPolicyClient}.
 *
 * @since 1.101
 */
@Named
public class CloudRestClientFactory
    extends RestClientFactory
{
  public RestClient newRestCLIClient(CloudParameters parameters, Configuration config) {
    return new CloudRestCLIClient(parameters, config);
  }

  /**
   * @since 1.101
   */
  public static class CloudRestCLIClient
      extends RestClient
  {
    private final CloudParameters parameters;

    CloudRestCLIClient(final CloudParameters parameters, Configuration config) {
      super(config);
      this.parameters = parameters;
    }

    @Override
    public PolicyEvaluationPollingResult evaluatePolicy(String appId,
                                                        String stageId,
                                                        final ClientScanResult clientScanResult,
                                                        final ClientScanType clientScanType) throws IOException
    {
      return new CloudPolicyClient(parameters, config, appId)
          .evaluateCLI(clientScanResult, clientScanType, new Stage(stageId));
    }

    @Override
    public ScanReceipt uploadScan(String appId, File scanFile, ClientScanType clientScanType) throws IOException {
      return new ScanClient(config, appId).uploadCLIScan(scanFile, clientScanType);
    }
  }
}

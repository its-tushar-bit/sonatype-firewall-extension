/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.brain.client.PolicyClient;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

/**
 * Cloud {@link PolicyClient} that handles client access for Cloud Policy Evaluation.
 *
 * @since 1.101
 */
public class CloudPolicyClient
    extends PolicyClient
{
  private static final Logger log = LoggerFactory.getLogger(CloudPolicyClient.class);

  private final CloudParameters parameters;

  public CloudPolicyClient(final CloudParameters parameters,
                           final Configuration config,
                           final String appId)
  {
    super(config, appId);
    this.parameters = parameters;
  }

  /**
   * Handles sending the {@link CloudParameters#getWaivers()} before {@link PolicyClient#pollEvaluationResult}.
   */
  @Override
  protected void beforePolling(final PolicyEvaluationReceipt receipt,
                               final String integrationPath) throws IOException
  {
    super.beforePolling(receipt, integrationPath);

    if (parameters.hasWaivers()) {
      log.info("Adding waivers for policy evaluation to before polling");

      Result result = path(
          "rest/integration/applications/",
          appId,
          "/evaluations/waivers/",
          receipt.getStatusId())
          .post(createWaiversEntity(this.parameters));

      verifyStatusCode(result);
    }
  }

  @Override
  protected RequestBuilder evaluationRequestPathBuilder(final String integrationPath,
                                                        final ClientScanType clientScanType,
                                                        final Stage stage)
  {
    RequestBuilder requestBuilder = super.evaluationRequestPathBuilder(integrationPath, clientScanType, stage);
    return parameters.hasWaivers() ? withScanTypeAndWaiverBuilder(requestBuilder, clientScanType) : requestBuilder;
  }

  protected RequestBuilder withScanTypeAndWaiverBuilder(final RequestBuilder requestBuilder,
                                                        final ClientScanType clientScanType)
  {
    return requestBuilder.query("scanType", clientScanType.name(), "withWaivers", "true");
  }

  protected StringEntity createWaiversEntity(final CloudParameters parameters) {
    return new StringEntity("{\"waivers\":" + parameters.getWaivers() + "}", APPLICATION_JSON);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyEvaluator<PARAMETERS extends AbstractCliParameters>
    extends AbstractPolicyEvaluator<PARAMETERS>
{

  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);

  @Inject
  public PolicyEvaluator(Scanner scanner, RestClientFactory restClientFactory) {
    super(scanner, restClientFactory);
  }

  @Override
  protected void processResults(PARAMETERS params,
                                ScanReceipt receipt,
                                PolicyEvaluationResult eval,
                                PolicyAction outcome,
                                RestClient restClient) throws ExitException
  {
    String reportUrl = receipt.resolveReportUrl(params.getServerUrl());

    saveResultFile(params, restClient, receipt);

    if (!PolicyAction.NONE.equals(outcome)) {
      log.info("");
      log.info("");
    }
    log.info("*********************************************************************************************");
    log.info("Policy Action: {}", outcome);
    log.info("Stage: {}", params.getStage().getStageTypeId());
    log.info("Summary of policy violations: {} critical, {} severe, {} moderate", eval.getCriticalComponentCount(),
        eval.getSevereComponentCount(), eval.getModerateComponentCount());
    log.info("The detailed report can be viewed online at {}", reportUrl);
    log.info("*********************************************************************************************");

    if (outcome.equals(PolicyAction.FAIL)) {
      throw new ExitException(1, "The IQ Server reports policy failing.");
    }
    else if (outcome.equals(PolicyAction.WARN) && params.isFailOnPolicyWarning()) {
      throw new ExitException(1, "The IQ Server reports policy warning.");
    }
  }

  private void saveResultFile(PARAMETERS params, RestClient restClient, ScanReceipt receipt) throws ExitException {
    if (params.getResultFile() != null) {
      try {
        restClient.saveResults(params.getApplicationId(), params.getResultFile(), receipt);
      }
      catch (IOException e) {
        log.error("The policy evaluation results could not be exported to {}", params.getResultFile(), e);
        throw new ExitException(params.isIgnoreSystemErrors(), e);
      }
    }
  }

  @Override
  protected RestClient createClient(Configuration configuration) {
    return restClientFactory.newRestCLIClient(configuration);
  }
}

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
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.10
 */
@Named
public class PolicyEvaluator
    extends AbstractPolicyEvaluator<Parameters>
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);

  @Inject
  public PolicyEvaluator(Scanner scanner, RestClientFactory restClientFactory) {
    super(scanner, restClientFactory);
  }

  @Override
  protected void processResults(Parameters params,
                                ScanReceipt receipt,
                                PolicyEvaluationResult eval,
                                PolicyAction outcome,
                                RestClient restClient) throws ExitException
  {
    saveReportBundleFile(params, restClient, receipt);

    if (!PolicyAction.NONE.equals(outcome)) {
      log.info("");
      log.info("");
    }
    log.info("*********************************************************************************************");
    log.info("Policy Action: {}", outcome);
    log.info("Stage: {}", params.getStage().getStageTypeId());
    log.info("Summary of policy violations: {} critical, {} severe, {} moderate", eval.getCriticalComponentCount(),
        eval.getSevereComponentCount(), eval.getModerateComponentCount());
    log.info("The report bundle was downloaded to {}", params.getReportBundleFile());
    log.info("*********************************************************************************************");

    if (outcome.equals(PolicyAction.FAIL)) {
      throw new ExitException(1, "The IQ Server reports policy failing.");
    }
  }

  private void saveReportBundleFile(Parameters params, RestClient restClient, ScanReceipt receipt)
      throws ExitException
  {
    log.info("Downloading report bundle from the IQ Server...");
    try {
      restClient.saveReportBundle(params.getApplicationId(), receipt.getScanId(), params.getReportBundleFile());
    }
    catch (IOException e) {
      log.error("The report bundle could not be downloaded to {}", params.getReportBundleFile(), e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  @Override
  protected ClientScanType getClientScanType() {
    return ClientScanType.SONATYPE;
  }
}

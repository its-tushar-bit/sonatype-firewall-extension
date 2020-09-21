/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.IOException;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.model.ClientScanType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PolicyEvaluator<P extends AbstractCliParameters>
    extends AbstractPolicyEvaluator<P>
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);

  PolicyEvaluator(Scanner scanner, RestClientFactory restClientFactory) {
    super(scanner, restClientFactory);
  }

  @Override
  protected void processResults(P params,
                                ScanReceipt receipt,
                                PolicyEvaluationResult eval,
                                PolicyAction outcome,
                                RestClient restClient) throws ExitException
  {
    String reportUrl = receipt.resolveReportUrl(params.getServerUrl());

    saveResultFile(params, restClient, receipt, eval, outcome);

    if (!PolicyAction.NONE.equals(outcome)) {
      log.info("");
      log.info("");
    }
    log.info("*********************************************************************************************");
    log.info("Policy Action: {}", outcome);
    log.info("Stage: {}", params.getStage().getStageTypeId());
    log.info("Number of components affected: {} critical, {} severe, {} moderate", eval.getCriticalComponentCount(),
        eval.getSevereComponentCount(), eval.getModerateComponentCount());
    log.info("Number of open policy violations: {} critical, {} severe, {} moderate",
        eval.getCriticalPolicyViolationCount(), eval.getSeverePolicyViolationCount(),
        eval.getModeratePolicyViolationCount());
    log.info("Number of grandfathered policy violations: {}", eval.getGrandfatheredPolicyViolationCount());
    log.info("Number of components: {}", eval.getTotalComponentCount());
    log.info("The detailed report can be viewed online at {}", reportUrl);
    log.info("*********************************************************************************************");

    if (outcome.equals(PolicyAction.FAIL)) {
      throw new ExitException(1, "The IQ Server reports policy failing.");
    }
    else if (outcome.equals(PolicyAction.WARN) && params.isFailOnPolicyWarning()) {
      throw new ExitException(1, "The IQ Server reports policy warning.");
    }
  }

  private void saveResultFile(
      P params,
      RestClient restClient,
      ScanReceipt receipt,
      PolicyEvaluationResult eval,
      PolicyAction outcome) throws ExitException
  {
    if (params.getResultFile() != null) {
      try {
        restClient.saveResults(params.getApplicationId(), params.getResultFile(), receipt, eval, outcome.toString());
      }
      catch (IOException e) {
        log.error("The policy evaluation results could not be exported to {}", params.getResultFile(), e);
        throw new ExitException(params.isIgnoreSystemErrors(), e);
      }
    }
  }

  @Override
  protected void saveErrorData(P params, CLIError error, RestClient restClient) throws ExitException {
    if (params.getResultFile() != null) {
      try {
        restClient.saveErrorData(
            params.getApplicationId(), params.getResultFile(), error.getErrorMessage(), error.isSystemError());
      }
      catch (IOException e) {
        log.error("The policy evaluation error data could not be exported to {}", params.getResultFile(), e);
        throw new ExitException(params.isIgnoreSystemErrors(), e);
      }
    }
  }

  @Override
  protected RestClient createClient(Configuration configuration) {
    return restClientFactory.newRestCLIClient(configuration);
  }

  @Override
  protected ClientScanType getClientScanType() {
    return ClientScanType.SONATYPE;
  }
  
  @Override
  public void run(P params) throws ExitException {
    validateAuthenticationConfig(params);
    super.run(params);
  }
  
  private void validateAuthenticationConfig(final P params) throws ExitException {
    if (params.isPkiAuthentication() && params.getServerUser() != null) {
      String message = "Only one mode of authentication can be enabled at a time"
          + ", --authentication and --pki-authentication are mutually exclusive.";
      log.error(message);
      throw new ExitException(1, message);
    }
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.34
 */
@Named
public class ExpandedCoveragePolicyEvaluator
    extends PolicyEvaluator<Parameters>
{
  static final String EXPANDED_COVERAGE_UNSUPPORTED_MESSAGE =
      "   Lifecycle XC is no longer supported. Please refer " +
          "to https://links.sonatype.com/products/nxiq/guides/lifecycle-scanning on what to use instead.";

  private static final Logger log = LoggerFactory.getLogger(ExpandedCoveragePolicyEvaluator.class);

  @Inject
  public ExpandedCoveragePolicyEvaluator(Scanner scanner, RestClientFactory restClientFactory) {
    super(scanner, restClientFactory);
  }

  @Override
  protected ClientScanResult scan(Parameters params,
                                  ProprietaryConfig proprietaryConfig,
                                  RestClient restClient) throws ExitException
  {
    log.info("");
    log.info("***********************************************************" +
            "*************************************************************************************************");
    log.info(EXPANDED_COVERAGE_UNSUPPORTED_MESSAGE);
    log.info("***********************************************************" +
            "*************************************************************************************************");
    log.info("");
    throw new ExitException(0);
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
    throw new UnsupportedOperationException("Lifecycle XC is no longer supported");
  }

  @Override
  protected ClientScanType getClientScanType() {
    throw new UnsupportedOperationException("Lifecycle XC is no longer supported");
  }
}

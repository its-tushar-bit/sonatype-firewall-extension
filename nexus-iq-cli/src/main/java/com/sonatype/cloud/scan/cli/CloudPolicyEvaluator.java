/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.cli.AbstractParameters;
import com.sonatype.insight.scan.cli.AbstractPolicyEvaluator;
import com.sonatype.insight.scan.cli.CLIError;
import com.sonatype.insight.scan.cli.ExitException;
import com.sonatype.insight.scan.cli.PolicyEvaluator;
import com.sonatype.insight.scan.cli.Scanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * {@link PolicyEvaluator} implementation for Cloud, with the use of custom {@link CloudParameters}.
 *
 * @since 1.101
 */
@Named
public class CloudPolicyEvaluator
    extends PolicyEvaluator<CloudParameters>
{
  private static final Logger log = LoggerFactory.getLogger(CloudPolicyEvaluator.class);

  @Inject
  public CloudPolicyEvaluator(final Scanner scanner,
                              final CloudRestClientFactory restClientFactory)
  {
    super(scanner, restClientFactory);
  }

  /**
   * Similar to {@link AbstractPolicyEvaluator#createClient(AbstractParameters)}, but creates a {@link RestClient}
   * that is cloud friendly by {@link CloudParameters} given.
   *
   * @return RestClient
   */
  @Override
  protected RestClient createClient(final CloudParameters params) {
    return ((CloudRestClientFactory) restClientFactory).newRestCLIClient(params, newHttpClientConfig(params));
  }

  /**
   * Validate params, specifically waivers in this overridden method.
   *
   * @see AbstractPolicyEvaluator#validate(AbstractParameters, RestClient)
   */
  @Override
  protected void validate(final CloudParameters params, final RestClient restClient) throws ExitException {
    super.validate(params, restClient);

    validateWaivers(params, restClient);
  }

  protected void validateWaivers(final CloudParameters params, final RestClient restClient) throws ExitException {
    if (!params.hasWaivers()) {
      return;
    }

    String waivers = params.getWaivers();
    log.info("Validating waivers input {}...", waivers);

    try {
      new ObjectMapper().readValue(waivers, new TypeReference<List<CloudPolicyWaiverDTO>>()
      {
      });
    }
    catch (JsonProcessingException e) {
      String message = format("The waiver input is invalid JSON for waiving: %s", e.getMessage());
      log.error(message);
      log.debug("Error details below:", e);
      saveErrorData(params, CLIError.forSystemError(e.getMessage()), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }
}

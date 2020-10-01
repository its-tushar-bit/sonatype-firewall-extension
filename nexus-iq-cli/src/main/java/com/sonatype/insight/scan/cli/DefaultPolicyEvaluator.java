/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.client.RestClientFactory;

/**
 * @since 1.34
 */
@Named
public class DefaultPolicyEvaluator
    extends PolicyEvaluator
{
  @Inject
  public DefaultPolicyEvaluator(final Scanner scanner, final RestClientFactory restClientFactory) {
    super(scanner, restClientFactory);
  }
}

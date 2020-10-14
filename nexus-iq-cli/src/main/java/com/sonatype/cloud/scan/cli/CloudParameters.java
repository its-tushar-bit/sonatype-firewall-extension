/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

import com.sonatype.insight.scan.cli.Parameters;

import com.beust.jcommander.Parameter;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Additional Parameters class to extend on the default ones in {@link Parameters} to
 * allow the adding of waivers input.
 *
 * @since 1.101
 */
public class CloudParameters
    extends Parameters
{
  /**
   * @since 1.101
   */
  @Parameter(names = {"--waivers"}, description = "Input waivers to use during Policy Evaluation, in JSON format:\n" +
      "      {" +
      "      \"waivers\": [{\n" +
      "          \"hash\": \"hash\",\n" +
      "          \"policyId\": \"id\"" +
      "       }]\n" +
      "      }")
  private String waivers;

  public CloudParameters(final String... args) {
    super(args);
  }

  public String getWaivers() {
    return waivers;
  }

  public boolean hasWaivers() {
    return isNotBlank(waivers);
  }
}

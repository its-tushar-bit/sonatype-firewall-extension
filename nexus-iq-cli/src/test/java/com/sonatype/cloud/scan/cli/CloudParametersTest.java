/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudParametersTest
{
  @Test
  public void testGetCloudPolicyEvaluatorClass_WaiverParameter() {
    assertThat(new CloudParameters("--waivers", "something").getWaivers()).isNotBlank();
    assertThat(new CloudParameters().getWaivers()).isNull();
  }
}

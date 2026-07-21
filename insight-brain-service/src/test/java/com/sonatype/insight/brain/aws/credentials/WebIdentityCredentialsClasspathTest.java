/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aws.credentials;

import org.junit.Test;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThatCode;

public class WebIdentityCredentialsClasspathTest
{
  @Test
  public void testStsWebIdentityProvider_isOnClassPath() {
    assertThatCode(StsAssumeRoleWithWebIdentityCredentialsProvider::builder)
        .doesNotThrowAnyException();
  }
}

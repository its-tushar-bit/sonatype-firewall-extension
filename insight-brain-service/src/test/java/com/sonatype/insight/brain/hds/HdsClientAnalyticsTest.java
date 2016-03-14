/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class HdsClientAnalyticsTest
{
  @Test
  public void testApplicationIdIsObfuscated() throws Exception {
    String appId = "test-app-id";
    HdsClientAnalytics analytics = HdsClientAnalytics.forApplication(appId);

    assertThat(analytics.getOwnerId(), is(notNullValue()));
    assertThat(analytics.getOwnerId(), is(not(appId)));
  }

  /**
   * Obfuscation must use an algorithm where the original value cannot be derived from the obfuscated value.
   *
   * We've decided to use SHA1 hashing.  The test ensures this remains the case because a different value would affect
   * the analytics reporting.
   */
  @Test
  public void testObfuscationUsesSha1() {
    String appId = "test-app-id";
    String appIdAsSha1 = "932742edc45df7e2d66eee12b3fb751621660dcb";

    HdsClientAnalytics analytics = HdsClientAnalytics.forApplication(appId);

    assertThat(analytics.getOwnerId(), is(appIdAsSha1));
  }
}

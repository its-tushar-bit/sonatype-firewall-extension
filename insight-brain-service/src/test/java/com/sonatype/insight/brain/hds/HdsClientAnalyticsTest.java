/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HdsClientAnalyticsTest
{
  @Test
  public void testApplicationIdIsObfuscated() throws Exception {
    String appId = "test-app-id";
    HdsClientAnalytics analytics = HdsClientAnalytics.forApplication(appId);

    assertThat(analytics.getOwnerId()).isNotNull();
    assertThat(analytics.getOwnerId()).isNotEqualTo(appId);
    assertThat(analytics.getOwnerType()).isEqualTo(OwnerType.APPLICATION);
  }

  @Test
  public void testRepositoryIdIsObfuscated() throws Exception {
    Owner owner = new Repository("my-repo-man-id", "central");
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(owner);

    assertThat(analytics.getOwnerId()).isNotNull();
    assertThat(analytics.getOwnerId()).isNotEqualTo("central");
    assertThat(analytics.getOwnerType()).isEqualTo(OwnerType.REPOSITORY);
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

    assertThat(analytics.getOwnerId()).isEqualTo(appIdAsSha1);
  }
}

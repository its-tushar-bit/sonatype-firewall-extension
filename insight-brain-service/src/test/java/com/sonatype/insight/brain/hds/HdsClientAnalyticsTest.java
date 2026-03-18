/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HdsClientAnalyticsTest
{
  @Test
  public void testApplicationIdIsObfuscated() {
    Application app = new Application();
    app.setId("test-app-id");
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(app);

    assertThat(analytics.getOwnerId()).matches("[0-9a-fA-F]{40}");
    assertThat(analytics.getOwnerType()).isEqualTo(OwnerType.APPLICATION);
  }

  @Test
  public void testRepositoryIdIsObfuscated() {
    Repository repo = new Repository();
    repo.setId("test-repo-id");
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(repo);

    assertThat(analytics.getOwnerId()).matches("[0-9a-fA-F]{40}");
    assertThat(analytics.getOwnerType()).isEqualTo(OwnerType.REPOSITORY);
  }

  /**
   * Obfuscation must use an algorithm where the original value cannot be derived from the obfuscated value.
   *
   * We've decided to use SHA1 hashing. The test ensures this remains the case because a different value would affect
   * the analytics reporting.
   */
  @Test
  public void testObfuscationUsesSha1() {
    Application app = new Application();
    app.setId("test-app-id");
    String appIdAsSha1 = "932742edc45df7e2d66eee12b3fb751621660dcb";

    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(app);

    assertThat(analytics.getOwnerId()).isEqualTo(appIdAsSha1);
  }
}

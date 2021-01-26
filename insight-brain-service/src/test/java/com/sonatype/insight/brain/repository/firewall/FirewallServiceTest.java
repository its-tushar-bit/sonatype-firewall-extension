/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirewallServiceTest
    extends AbstractComponentTest
{
  @Inject
  FirewallService firewallService;

  @Inject
  private InsightConfig config;

  @Test
  public void testFeatureFlag() {
    //set experimental feature
    config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    // expect feature flag to be true
    assertThat(firewallService.getFirewallStatus().experimentalFeatures)
        .containsEntry(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true);
  }

  @Test
  public void testFeatureFlag_False() {
    // expect feature flag to be false
    assertThat(firewallService.getFirewallStatus().experimentalFeatures)
        .containsEntry(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false);
  }
}

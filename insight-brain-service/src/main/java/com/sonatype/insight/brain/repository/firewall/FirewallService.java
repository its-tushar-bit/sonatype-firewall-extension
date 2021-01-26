/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

/**
 * @since 1.105.0
 */
@Named
public class FirewallService
{
  private final InsightConfig insightConfig;

  @Inject
  public FirewallService(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Authorize(permission = Permission.READ)
  public FirewallStatusDTO getFirewallStatus() {
    FirewallStatusDTO firewallStatusDTO = new FirewallStatusDTO();
    firewallStatusDTO.experimentalFeatures
        .put(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(),
            insightConfig.isExperimentalFeatureEnabled(Feature.FIREWALL_AUTO_UNQUARANTINE));
    return firewallStatusDTO;
  }
}

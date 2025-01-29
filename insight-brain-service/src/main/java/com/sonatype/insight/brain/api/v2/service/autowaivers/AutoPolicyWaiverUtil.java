/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import org.apache.shiro.authz.UnauthorizedException;

public class AutoPolicyWaiverUtil
{
  public static void validateAutoWaiversFeatureEnabled() {
    if (!SystemConfigurationPropertyFeature.AUTO_WAIVERS.isEnabled()) {
      throw new UnauthorizedException("Auto Policy Waivers feature is not enabled");
    }
  }
}

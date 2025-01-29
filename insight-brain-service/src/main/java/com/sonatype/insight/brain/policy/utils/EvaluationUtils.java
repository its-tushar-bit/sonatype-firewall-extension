/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.utils;

import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import org.apache.shiro.authz.UnauthorizedException;

public class EvaluationUtils
{
  public static ScanTriggerType getScanTriggerType(IntegrationType integrationType) {
    switch (integrationType) {
      case CI:
        return ScanTriggerType.CONTINUOUS_INTEGRATION;
      case CLI:
        return ScanTriggerType.CLI;
      case RM:
        return ScanTriggerType.REPOSITORY_MANAGER;
      default:
        throw new IllegalArgumentException("Unknown integration type " + integrationType);
    }
  }

  public static void ensureNewEvaluationProcessEnabled() {
    if (!SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.isEnabled()) {
      throw new UnauthorizedException(
          SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS.getId() + " feature is disabled.");
    }
  }
}

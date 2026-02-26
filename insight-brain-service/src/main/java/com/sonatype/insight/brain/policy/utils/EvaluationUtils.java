/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.utils;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;

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

  /**
   * Determines if a policy violation was remediated by a version change (upgrade or downgrade).
   * Returns true if the component that caused the violation is still present but with a different
   * version (the old version is not in the found components list).
   *
   * @param foundComponents the components found in the new scan that match the violation's component identifier
   * @param oldPolicyViolation the policy violation that was fixed
   * @return true if the fix was due to a version change, false otherwise
   */
  public static boolean isRemediatedByVersionChange(List<Component> foundComponents,
      PolicyViolation oldPolicyViolation)
  {
    if (foundComponents == null || foundComponents.isEmpty()) {
      return false;
    }

    ComponentIdentifier componentIdentifier = oldPolicyViolation.getComponentIdentifier();
    if (componentIdentifier == null) {
      return false;
    }

    String oldVersion = componentIdentifier.get(ComponentIdentifier.VERSION);
    if (oldVersion == null) {
      return false;
    }

    boolean hasNullVersion = foundComponents.stream()
        .anyMatch(c -> c.getVersion() == null);
    if (hasNullVersion) {
      return false;
    }

    boolean oldVersionPresent = foundComponents.stream()
        .anyMatch(c -> oldVersion.equals(c.getVersion()));

    return !oldVersionPresent;
  }
}

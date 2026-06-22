/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;

/**
 * Test-only shim that exposes the package-private add* methods on {@link PolicyResults}
 * so unit tests in {@code com.sonatype.insight.brain.guide.policy} can hand-craft a
 * {@link PolicyResults} without going through the full Drools evaluator. Production
 * code never sees this class.
 */
public final class PolicyResultsAccess
{
  private PolicyResultsAccess() {
  }

  public static void addActiveAlert(PolicyResults results, PolicyAlert alert) {
    results.addActiveAlert(alert);
  }

  public static void addWaivedAlert(PolicyResults results, PolicyAlert alert) {
    results.addWaivedAlert(alert);
  }

  public static void addPolicyWaiver(PolicyResults results, ComponentFact cf, PolicyWaiver waiver) {
    results.addPolicyWaiver(cf, waiver);
  }
}

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.List;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;

/**
 * Shared test fixture: builds a single FAIL-action {@link PolicyAlert} that produces a
 * blocked verdict when run through {@link HostedEvaluationResultMapper}. Used by the
 * sync evaluation service test.
 */
final class BlockedAlertFixture
{
  private BlockedAlertFixture() {
  }

  static PolicyAlert build() {
    PolicyFact trigger = new PolicyFact("policy-critical-security", "Critical Security Policy", 9);
    TreeMap<String, String> coords = new TreeMap<>();
    coords.put("groupId", "com.acme");
    coords.put("artifactId", "lib");
    coords.put("version", "1.2.3");
    ComponentFact componentFact = new ComponentFact(
        new ComponentIdentifier("maven", coords),
        "abc123def456ghi7");
    componentFact.addPathnames(List.of("com/acme/lib/1.2.3/lib-1.2.3.jar"));
    componentFact.addConstraintFact(
        new ConstraintFact("constraint-cvss-critical", "Critical CVSS", "CVSS >= 9.0"));
    trigger.addComponentFact(componentFact);
    return new PolicyAlert(trigger, List.of(new Action(Action.ID_FAIL)));
  }
}

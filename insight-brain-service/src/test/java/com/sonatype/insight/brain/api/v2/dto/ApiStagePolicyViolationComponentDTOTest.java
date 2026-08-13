/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiStagePolicyViolationComponentDTOTest
{
  @Test
  public void testFromPolicyViolation() {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setPolicyId("policyId");
    policyViolation.setPolicyName("policyName");
    policyViolation.setThreatLevel(9);
    policyViolation.setThreatCategory(PolicyThreatCategory.SECURITY);
    policyViolation.setId("id");
    policyViolation.setActionTypeId("actionTypeId");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    policyViolation.setComponentIdentifier(componentIdentifier);
    Component component = new Component();
    component.setHash("hash");
    component.setDisplayName("g : a : v");

    ApiStagePolicyViolationComponentDTO result =
        ApiStagePolicyViolationComponentDTO.fromPolicyViolationAndComponent(Pair.of(policyViolation, component));

    assertThat(result.policyId).isEqualTo(policyViolation.getPolicyId());
    assertThat(result.policyName).isEqualTo(policyViolation.getPolicyName());
    assertThat(result.threatLevel).isEqualTo(policyViolation.getThreatLevel());
    assertThat(result.threatCategory).isEqualTo(policyViolation.getThreatCategory().getName());
    assertThat(result.policyViolationId).isEqualTo(policyViolation.getId());
    assertThat(result.action).isEqualTo(policyViolation.getActionTypeId());
    assertThat(result.componentIdentifier)
        .usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.toPackageUrl(componentIdentifier));
    assertThat(result.hash).isEqualTo(component.getHash());
    assertThat(result.displayName).isEqualTo(component.getDisplayName());
  }

  @Test
  public void testFromPolicyViolation_Minimal() {
    PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.setThreatCategory(PolicyThreatCategory.SECURITY);

    ApiStagePolicyViolationComponentDTO result =
        ApiStagePolicyViolationComponentDTO.fromPolicyViolationAndComponent(Pair.of(policyViolation, new Component()));

    assertThat(result.threatCategory).isEqualTo(policyViolation.getThreatCategory().getName());
  }
}

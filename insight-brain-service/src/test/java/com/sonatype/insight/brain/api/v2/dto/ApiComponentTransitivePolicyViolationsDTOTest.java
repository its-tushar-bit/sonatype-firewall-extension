/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentTransitivePolicyViolationsDTOTest
{
  @Test
  public void testFromPolicyViolations_Empty() {
    Component component = new Component();
    component.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));
    component.setInnerSource(true);
    component.setHash("hash");
    component.setDisplayName("g : a : v");

    ApiComponentTransitivePolicyViolationsDTO result = new ApiComponentTransitivePolicyViolationsDTO(
        component, Collections.emptyList());

    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(component.getComponentIdentifier()));
    assertThat(result.packageUrl)
        .isEqualTo(PackageUrlIdentifier.toPackageUrl(component.getComponentIdentifier()));
    assertThat(result.hash).isEqualTo(component.getHash());
    assertThat(result.displayName).isEqualTo(component.getDisplayName());
    assertThat(result.isInnerSource).isTrue();
    assertThat(result.transitivePolicyViolations).isEmpty();
  }

  @Test
  public void testFromPolicyViolations() {
    Component component = new Component();
    PolicyViolation policyViolation1 = new PolicyViolation();
    policyViolation1.setThreatCategory(PolicyThreatCategory.SECURITY);
    PolicyViolation policyViolation2 = new PolicyViolation();
    policyViolation2.setThreatCategory(PolicyThreatCategory.LICENSE);

    ApiComponentTransitivePolicyViolationsDTO result = new ApiComponentTransitivePolicyViolationsDTO(
        component,
        Arrays.asList(Pair.of(policyViolation1, new Component()), Pair.of(policyViolation2, new Component())));

    assertThat(result).isNotNull();
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(policyViolation1, new Component())),
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(policyViolation2, new Component())));
  }
}

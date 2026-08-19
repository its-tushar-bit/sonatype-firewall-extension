/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationDTOComparatorTest
{
  @Test
  public void testPolicyViolationDTOComparison() {
    PolicyViolationDTO dto9AA = buildPolicyViolationDTO(9, "A", "A", null, null);
    PolicyViolationDTO dto8AA = buildPolicyViolationDTO(8, "A", "A", null, null);
    PolicyViolationDTO dto8BA = buildPolicyViolationDTO(8, "B", "A", null, null);
    PolicyViolationDTO dto8BB = buildPolicyViolationDTO(8, "B", "B", null, null);
    PolicyViolationDTO dto7AAAAA = buildPolicyViolationDTO(7, "A", "A",
        ComponentIdentifier.createMavenCoordinates("A", "A", "A"), null);
    PolicyViolationDTO dto7AAAAB = buildPolicyViolationDTO(7, "A", "A",
        ComponentIdentifier.createMavenCoordinates("A", "A", "B"), null);
    PolicyViolationDTO dto7AAA_NULL_A = buildPolicyViolationDTO(7, "A", "A",
        ComponentIdentifier.createMavenCoordinates("A", null, "A"), null);
    PolicyViolationDTO dto7AAA_NULL_B = buildPolicyViolationDTO(7, "A", "A",
        ComponentIdentifier.createMavenCoordinates("A", null, "B"), null);
    PolicyViolationDTO dto7BAAAB = buildPolicyViolationDTO(7, "B", "A",
        ComponentIdentifier.createMavenCoordinates("A", "A", "B"), null);
    PolicyViolationDTO dto7BAACA = buildPolicyViolationDTO(7, "B", "A",
        ComponentIdentifier.createMavenCoordinates("A", "C", "A"), null);
    PolicyViolationDTO dto7CCCCC_A = buildPolicyViolationDTO(7, "C", "C",
        ComponentIdentifier.createMavenCoordinates("C", "C", "C"), "A");
    PolicyViolationDTO dto7CCCCC_BB = buildPolicyViolationDTO(7, "C", "C",
        ComponentIdentifier.createMavenCoordinates("C", "C", "C"), "BB");
    PolicyViolationDTO dto7CCCCC_B = buildPolicyViolationDTO(7, "C", "C",
        ComponentIdentifier.createMavenCoordinates("C", "C", "C"), "B");
    PolicyViolationDTO dto7CCCCC_BA = buildPolicyViolationDTO(7, "C", "C",
        ComponentIdentifier.createMavenCoordinates("C", "C", "C"), "BA");

    List<PolicyViolationDTO> sorted = Lists.newArrayList(dto7CCCCC_A, dto7AAA_NULL_A, dto7BAACA, dto7CCCCC_BB, dto8BA,
        dto7BAAAB, dto9AA, dto7AAAAB, dto7CCCCC_B, dto7AAA_NULL_B, dto7CCCCC_BA, dto8AA, dto8BB, dto7AAAAA);

    sorted.sort(new PolicyViolationDTOComparator());

    List<PolicyViolationDTO> expected = Lists.newArrayList(dto9AA, dto8AA, dto8BA, dto8BB, dto7AAAAA, dto7AAAAB,
        dto7AAA_NULL_A, dto7AAA_NULL_B, dto7BAAAB, dto7BAACA, dto7CCCCC_A, dto7CCCCC_B, dto7CCCCC_BA, dto7CCCCC_BB);

    assertThat(sorted).isEqualTo(expected);
  }

  private PolicyViolationDTO buildPolicyViolationDTO(
      int threatLevel,
      String policyName,
      String applicationName,
      ComponentIdentifier componentIdentifier,
      String hash)
  {
    PolicyViolationDTO dto = new PolicyViolationDTO();
    dto.id = UUID.randomUUID().toString();
    dto.policyName = policyName;
    dto.applicationName = applicationName;
    dto.threatLevel = threatLevel;
    dto.componentIdentifier = componentIdentifier;
    dto.hash = hash;
    return dto;
  }

  @Test
  public void testIndistinguishable_NoComponentIdentifierNoHash() {
    PolicyViolationDTO dto1 = buildPolicyViolationDTO(7, "policy", "app", null, null);
    PolicyViolationDTO dto2 = buildPolicyViolationDTO(7, "policy", "app", null, null);

    List<PolicyViolationDTO> sorted = Lists.newArrayList(dto1, dto2);

    sorted.sort(new PolicyViolationDTOComparator());

    // sanity check, the key point is merely to not blow up with an NPE
    assertThat(sorted).containsExactly(dto1, dto2);
  }

  @Test
  public void testCompareMavenNuget() {
    PolicyViolationDTO dto1 = buildPolicyViolationDTO(7, "policy", "app",
        ComponentIdentifier.createMavenCoordinates("A", "B", "C"), null);
    PolicyViolationDTO dto2 = buildPolicyViolationDTO(7, "policy", "app",
        ComponentIdentifier.createNugetCoordinates("A", "B"), null);

    PolicyViolationDTOComparator comparator = new PolicyViolationDTOComparator();
    assertThat(comparator.compare(dto1, dto2)).isEqualTo(-1);
    assertThat(comparator.compare(dto2, dto1)).isEqualTo(1);
  }
}
